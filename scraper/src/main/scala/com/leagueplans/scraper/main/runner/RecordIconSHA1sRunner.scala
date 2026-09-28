package com.leagueplans.scraper.main.runner

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, ItemChangeset, ItemData}
import com.leagueplans.scraper.dumper.items.ItemDumper
import com.leagueplans.scraper.main.CommandLineArgs
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiContentType, WikiSelector}
import com.leagueplans.scraper.wiki.model.{FileInfo, PageDescriptor, WikiItem}
import com.leagueplans.scraper.wiki.scraper.ItemsScraper
import com.leagueplans.scraper.wiki.streaming.*
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import zio.{Chunk, RIO, Scope, Task, Trace, ZIO}

import java.nio.file.{Files, Path}
import java.time.{Duration, Instant}
import scala.util.Try

/** A one-off migration that records the wiki's SHA-1 against every accepted icon it can
  * vouch for, without downloading any of them.
  *
  * The last applied scrape compared every icon it downloaded against the accepted data, so
  * an accepted icon matching what that scrape saw was the picture the wiki served at the
  * time. The wiki's SHA-1 for the file can be taken for it, provided nothing has changed
  * since: neither the item's page, which decides which file each icon comes from, nor the
  * file itself.
  *
  * Anything that can't be vouched for is left without a SHA-1, which costs nothing - the
  * next scrape downloads that icon as it always has, and records its SHA-1 then.
  *
  * Arguments:
  *  - `original-items`: the accepted item data
  *  - `applied-changeset`: the changeset of the last applied scrape
  *  - `scraped-at`: when that scrape started, as an ISO 8601 instant
  *
  * Writes the updated item data to `items.json` in the target directory.
  */
object RecordIconSHA1sRunner {
  /** Our downloads don't carry the wiki's cache-busting query, so a file re-uploaded shortly
    * before the scrape may have been served stale. Files that recent aren't vouched for.
    */
  private val uploadMargin = Duration.ofDays(1)

  def make(
    args: CommandLineArgs,
    targetDirectory: Path,
    client: WikiClient
  )(using Trace): Task[RecordIconSHA1sRunner] =
    for {
      accepted <- ZIO.fromTry(ItemDumper.loadOriginalData(args))
      applied <- ZIO.fromTry(args.get("applied-changeset")(path =>
        Try(Files.readString(Path.of(path))).flatMap(decode[ItemChangeset](_).toTry)
      ))
      scrapedAt <- ZIO.fromTry(args.get("scraped-at")(raw => Try(Instant.parse(raw))))
      scraper <- ZIO.fromTry(ItemsScraper.make(args, client, accepted.toMap))
    } yield RecordIconSHA1sRunner(
      accepted,
      applied,
      scrapedAt,
      scraper,
      client,
      targetDirectory.resolve("items.json")
    )
}

final class RecordIconSHA1sRunner(
  accepted: Vector[(InfoboxKey, ItemData)],
  applied: ItemChangeset,
  scrapedAt: Instant,
  scraper: ItemsScraper,
  client: WikiClient,
  output: Path
) extends ScrapeRunner {
  private type Current = (infoboxes: WikiItem.Infoboxes, files: Map[PageDescriptor.Name.File, FileInfo])

  def run(using Trace): RIO[Scope, Chunk[PageStream.Error]] =
    for {
      (fileErrors, scraped) <- scraper.scrapeFiles.pageRun
      current = scraped.map((_, item) => InfoboxKey(item._1.id, item._2.version.raw) -> (item._2, item._3)).toMap
      // Looked up after the pages' content, so that a page edited in between counts as edited.
      (editErrors, edits) <- lastEdits(scraped.map((_, item) => item._1).distinct)
      (updated, outcomes) = record(current, edits)
      _ <- ZIO.foreachDiscard(outcomes.groupMapReduce(identity)(_ => 1)(_ + _).toList.sortBy(-_._2))(
        (outcome, count) => ZIO.logInfo(s"$count icons: $outcome")
      )
      _ <- ZIO.attempt(Files.writeString(output, updated.asJson.noSpaces))
    } yield fileErrors ++ editErrors

  private def lastEdits(
    pages: Chunk[PageDescriptor]
  )(using Trace): RIO[Any, (Chunk[PageStream.Error], Map[Int, Instant])] =
    client
      .fetch(WikiSelector.Pages(pages.map(_.name).toVector), WikiContentType.LastEdited)
      .pageMapTry(raw => Try(Instant.parse(raw)))
      .pageRun
      .map((errors, edits) => (errors, edits.map((page, edited) => (page.id: Int) -> edited).toMap))

  private def record(
    current: Map[InfoboxKey, Current],
    edits: Map[Int, Instant]
  ): (Vector[(InfoboxKey, ItemData)], Vector[String]) = {
    val results =
      accepted.map((key, item) =>
        val images = item.images.map(image => image -> vouch(key, image, current, edits))
        val updatedImages = images.map {
          case (image, Right(sha1)) => image.copy(wikiSHA1 = Some(sha1))
          case (image, Left(_)) => image
        }
        ((key, item.copy(images = updatedImages)), images.toList.map(_._2.fold(identity, _ => "recorded")))
      )

    (results.map(_._1), results.flatMap(_._2))
  }

  /** The wiki's SHA-1 for an accepted icon, or why it can't be vouched for. */
  private def vouch(
    key: InfoboxKey,
    image: ItemData.Image,
    current: Map[InfoboxKey, Current],
    edits: Map[Int, Instant]
  ): Either[String, String] =
    for {
      _ <- Either.cond(image.wikiSHA1.isEmpty, (), "already recorded")
      seen <- seenByScrape(key).toRight("not compared by the scrape")
      _ <- Either.cond(seen.map(_.picture).contains(image.picture), (), "differs from what the scrape saw")
      edited <- edits.get(key.pageID).toRight("page's last edit unknown")
      _ <- Either.cond(!edited.isAfter(scrapedAt), (), "page edited since the scrape")
      now <- current.get(key).toRight("not found on the wiki now")
      file <- now.infoboxes.item.imageBins
        .find((bin, _) => bin.floor == image.bin.floor)
        .map(_._2)
        .toRight("bin no longer on the page")
      _ <- Either.cond(file.extension == image.extension, (), "file's extension changed")
      info <- now.files.get(file).toRight("file unknown to the wiki")
      _ <- Either.cond(!info.viaRedirect, (), "file reached through a redirect")
      _ <- Either.cond(
        info.uploaded.isBefore(scrapedAt.minus(RecordIconSHA1sRunner.uploadMargin)),
        (),
        "file uploaded within a day of the scrape, or since"
      )
    } yield info.sha1

  /** The icons the last scrape saw for an item: what the applied changeset recorded if it
    * mentions the item, and otherwise the accepted icons themselves, which the scrape found
    * unchanged. Items it withheld or reported removed were not compared at all.
    */
  private def seenByScrape(key: InfoboxKey): Option[List[ItemData.Image]] =
    if (notCompared.contains(key))
      None
    else
      reported.get(key).orElse(acceptedByKey.get(key).map(_.images)).map(_.toList)

  private lazy val acceptedByKey: Map[InfoboxKey, ItemData] =
    accepted.toMap

  private lazy val notCompared: Set[InfoboxKey] =
    (applied.withheld ++ applied.removed).map(_._1).toSet

  private lazy val reported: Map[InfoboxKey, NonEmptyList[ItemData.Image]] =
    (applied.added.map((key, item) => key -> item.images) ++
      applied.modified.map(change => change.key -> change.updated.images) ++
      applied.reimaged).toMap
}
