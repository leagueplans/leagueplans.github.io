package com.leagueplans.scraper.wiki.scraper

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, ItemData}
import com.leagueplans.scraper.main.CommandLineArgs
import com.leagueplans.scraper.wiki.decoder.items.{ItemPageDecoder, ItemPageObjectExtractor}
import com.leagueplans.scraper.wiki.http.{WikiClient, WikiContentType, WikiFetchException, WikiSelector}
import com.leagueplans.scraper.wiki.model.{FileInfo, Page, PageDescriptor, WikiItem}
import com.leagueplans.scraper.wiki.parser.TermParser
import com.leagueplans.scraper.wiki.streaming.*
import zio.stream.ZStream
import zio.{Chunk, Task, Trace, UIO, ZIO}

import scala.util.{Success, Try}

object ItemsScraper {
  def make(
    args: CommandLineArgs,
    client: WikiClient,
    accepted: Map[InfoboxKey, ItemData]
  ): Try[ItemsScraper] =
    parseMode(args).map(ItemsScraper(client, _, accepted))

  private def parseMode(args: CommandLineArgs): Try[ItemsScraper.Mode] =
    args.getOpt("pages") { input =>
      val pages = input.split('|').map[PageDescriptor.Name.Other](PageDescriptor.Name.Other.apply)
      Success(ItemsScraper.Mode.Pages(pages.toVector))
    }.map(_.getOrElse(ItemsScraper.Mode.All))

  private val infoboxSelector: WikiSelector =
    WikiSelector.PagesThatTransclude(PageDescriptor.Name.Template("Infobox Item"))

  private val ignoredCategories: Set[PageDescriptor.Name.Category] =
    Set(
      "Inaccessible items",
      "Interface items",
      "Needs examine added"
    ).map(PageDescriptor.Name.Category.apply)

  /** How many item versions have their icons' files looked up together. Most have one icon,
    * so a group this size usually fits in the wiki's limit of 50 names a request.
    */
  private val fileInfoGroupSize = 45

  enum Mode {
    case Pages(raw: Vector[PageDescriptor.Name.Other])
    case All
  }

  /** An item version, along with what the wiki reports about the files of its icons. */
  private type WithFiles = (PageDescriptor, WikiItem.Infoboxes, Map[PageDescriptor.Name.File, FileInfo])
}

/** @param accepted the item data already accepted, whose icons need not be downloaded again
  *                 while the wiki still holds the same files for them
  */
final class ItemsScraper(
  client: WikiClient,
  mode: ItemsScraper.Mode,
  accepted: Map[InfoboxKey, ItemData]
) {
  def scrape(using Trace): PageStream[WikiItem] =
    scrapeFiles.pageMapZIOPar(n = 8)((page, infoboxes, files) =>
      fetchImages(InfoboxKey(page.id, infoboxes.version.raw), infoboxes.item.imageBins, files)
        .map(WikiItem(infoboxes, _))
    )

  /** Every item version, along with what the wiki reports about the files of its icons. */
  private def scrapeFiles(using Trace): PageStream[ItemsScraper.WithFiles] = {
    val source = mode match {
      case ItemsScraper.Mode.Pages(pages) => fetch(pages)
      case ItemsScraper.Mode.All => fetchAll
    }

    source
      .pageMapEither(TermParser.parse)
      .pageMapZIO(ItemPageObjectExtractor.extract)
      .pageFlattenIterables
      .pageMapEither(identity)
      .pageExtend
      .pageMapEither((page, objects) => ItemPageDecoder.decode(page.name, objects))
      .pageExtend
      .grouped(ItemsScraper.fileInfoGroupSize)
      .mapZIO(withFileInfo)
      .flattenChunks
  }

  private def fetch(pages: Vector[PageDescriptor.Name.Other]): PageStream[String] =
    client.fetch(WikiSelector.Pages(pages), WikiContentType.Revisions)

  private def fetchAll(using Trace): PageStream[String] =
    ZStream
      .fromZIO(findPagesToIgnore)
      .flatMap((errors, ignoredPagesChunk) =>
        ZStream
          .fromIterable(errors.map(Left(_)))
          .concat(fetchIgnoring(ignoredPagesChunk.map((_, id) => id).toSet))
      )

  private def findPagesToIgnore(using Trace): UIO[(Chunk[PageStream.Error], Chunk[Page[PageDescriptor.ID]])] =
    ZStream
      .fromIterable(ItemsScraper.ignoredCategories)
      .flatMapPar(n = 4)(client.fetchAllMembers)
      .pageMap(_.id)
      .pageRun

  private def fetchIgnoring(ignoredPages: Set[PageDescriptor.ID])(using Trace): PageStream[String] =
    client
      .fetch(ItemsScraper.infoboxSelector, WikiContentType.Revisions)
      .filter {
        case Right((page, _)) => !ignoredPages.contains(page.id)
        case Left(_) => true
      }

  /** Looks up the files of a group of items' icons in as few requests as the wiki allows.
    *
    * Every icon has to be recorded with its SHA-1, so a lookup that fails fails every page
    * in the group, which are then withheld like any other page that couldn't be read.
    */
  private def withFileInfo(
    group: Chunk[Either[PageStream.Error, Page[(PageDescriptor, WikiItem.Infoboxes)]]]
  )(using Trace): UIO[Chunk[Either[PageStream.Error, Page[ItemsScraper.WithFiles]]]] = {
    val files =
      group.collect { case Right((_, (_, infoboxes))) => infoboxes.item.imageBins.toList.map(_._2) }.flatten

    client
      .fetchFileInfo(files)
      .either
      .map {
        case Right(infos) =>
          group.map(_.map { case (page, (descriptor, infoboxes)) => (page, (descriptor, infoboxes, infos)) })
        case Left(error) =>
          group.map(_.flatMap((page, _) => Left((page, error))))
      }
  }

  private def fetchImages(
    key: InfoboxKey,
    wikiBins: NonEmptyList[(Item.Image.Bin, PageDescriptor.Name.File)],
    files: Map[PageDescriptor.Name.File, FileInfo]
  )(using Trace): Task[NonEmptyList[WikiItem.Image]] = {
    val acceptedImages =
      accepted
        .get(key)
        .fold(Map.empty[Int, ItemData.Image])(_.images.toList.map(i => i.bin.floor -> i).toMap)

    ZIO.foreachPar(wikiBins.toList) { (bin, fileName) =>
      ZIO
        .fromOption(files.get(fileName).map(_.sha1))
        .orElseFail(WikiFetchException.NoFileInfo(fileName))
        .flatMap(wikiSHA1 =>
          acceptedImages.get(bin.floor) match {
            // The wiki's digest changes whenever a file is re-uploaded, so a match means the
            // accepted icon is still the one on the wiki.
            case Some(image) if image.wikiSHA1 == wikiSHA1 && image.extension == fileName.extension =>
              ZIO.logDebug("Skipping icon download: file unchanged since accepted")
                .as(WikiItem.Image(bin, fileName, wikiSHA1, WikiItem.Image.Content.Accepted(image.hash)))

            case _ =>
              client
                .fetchImage(fileName)
                .map(data => WikiItem.Image(bin, fileName, wikiSHA1, WikiItem.Image.Content.Downloaded(data)))
          }
        )
    }.map(NonEmptyList.fromListUnsafe)
  }
}
