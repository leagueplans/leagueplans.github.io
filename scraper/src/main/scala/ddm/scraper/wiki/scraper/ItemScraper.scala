package ddm.scraper.wiki.scraper

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.wiki.decoder.items.{ItemPageDecoder, ItemPageObjectExtractor}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.{Page, WikiItem}
import ddm.scraper.wiki.parser.TermParser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ItemScraper {
  private val infobox = "Infobox Item"
  private val ignoredCategories: Set[Page.Name.Category] =
    Set("Inaccessible items").map(Page.Name.Category.apply)

  enum Mode {
    /** Inclusive of the named page */
    case From(page: Page.Name.Other)
    case Pages(raw: List[Page.Name.Other])
    case All
  }

  def scrape(
    mode: Mode,
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  )(using mat: Materializer, ec: ExecutionContext): Source[(Page, WikiItem), ?] = {
    val source =
      mode match {
        case Mode.Pages(raw) =>
          client
            .fetch(MediaWikiSelector.Pages(raw), Some(MediaWikiContent.Revisions))
            .via(errorReportingFlow(reportError))

        case Mode.From(initialPage) =>
          Source
            .future(findPagesToIgnore(client))
            .flatMapConcat(ignoredPages => findItemPages(ignoredPages, client, reportError))
            .dropWhile((page, _) => page.name != initialPage)

        case Mode.All =>
          Source
            .future(findPagesToIgnore(client))
            .flatMapConcat(ignoredPages => findItemPages(ignoredPages, client, reportError))
      }

    source
      .mapConcat { (page, content) =>
        val (errors, infoboxes) = decode(page, content)
        errors.foreach(reportError(page, _))
        infoboxes.map((page, _))
      }
      .mapAsync(parallelism = 10)((page, infoboxes) =>
        fetchImages(infoboxes.item.imageBins, client).transform {
          case Failure(error) =>
            Success((page, Left(error)))
          case Success(images) =>
            Success((page, Right(WikiItem(infoboxes, images))))
        }
      )
      .via(errorReportingFlow(reportError))
  }

  private def errorReportingFlow[T](
    reportError: (Page, Throwable) => Unit
  ): Flow[(Page, Either[Throwable, T]), (Page, T), NotUsed] =
    Flow[(Page, Either[Throwable, T])]
      .collect(Function.unlift {
        case (page, Right(value)) =>
          Some((page, value))
        case (page, Left(error)) =>
          reportError(page, error)
          None
      })

  private def findPagesToIgnore(
    client: MediaWikiClient
  )(using materializer: Materializer): Future[Set[Page.ID]] =
    ignoredCategories
      .foldLeft(Source.empty[Page.ID])((acc, category) =>
        acc.concat(
          client
            .fetchAllMembers(category, maybeContent = None)
            .collect { case (Page(id, _: Page.Name.Other), _) => id }
        )
      )
      .runWith(Sink.collection)

  private def findItemPages(
    ignoredPages: Set[Page.ID],
    client: MediaWikiClient,
    reportError: (Page, Throwable) => Unit
  ): Source[(Page, String), ?] =
    client
      .fetch(
        MediaWikiSelector.PagesThatTransclude(Page.Name.Template(infobox)),
        Some(MediaWikiContent.Revisions)
      )
      .filterNot((page, _) => ignoredPages.contains(page.id))
      .via(errorReportingFlow(reportError))

  private def decode(page: Page, content: String): (List[Throwable], List[WikiItem.Infoboxes]) = {
    TermParser.parse(content) match {
      case Left(error) => (List(error), List.empty)
      case Right(terms) =>
        val (extractionErrors, versionedObjects) =
          ItemPageObjectExtractor.extract(terms).partitionMap(identity)

        val (decodingErrors, infoboxes) = versionedObjects.partitionMap(objects =>
          ItemPageDecoder.decode(page, objects)
        )

        (extractionErrors ++ decodingErrors, infoboxes)
    }
  }

  private def fetchImages(
    wikiBins: NonEmptyList[(Item.Image.Bin, Page.Name.File)],
    client: MediaWikiClient
  )(using ec: ExecutionContext): Future[NonEmptyList[WikiItem.Image]] =
    Future.sequence(
      wikiBins.map((bin, fileName) =>
        client.fetchImage(fileName).map(data => WikiItem.Image(bin, fileName, data))
      ).toList
    ).map(bins => NonEmptyList.fromListUnsafe(bins))
}
