package ddm.scraper.wiki.scraper

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.dumper.Cache
import ddm.scraper.reporter.Reporter
import ddm.scraper.wiki.decoder.{ItemInfoboxDecoder, ItemPageDecoder}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.{Page, WikiItem}
import ddm.scraper.wiki.parser.TermParser

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ItemScraper {
  private val infobox = "Infobox Item"
  private val ignoredCategories: Set[Page.Name.Category] =
    Set("Inaccessible items").map(Page.Name.Category)

  sealed trait Mode
  object Mode {
    /** Inclusive of the named page */
    final case class From(page: Page.Name.Other) extends Mode
    final case class Pages(raw: List[Page.Name.Other]) extends Mode
    case object All extends Mode
  }

  def scrape(
    mode: Mode,
    client: MediaWikiClient,
    reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]
  )(implicit mat: Materializer, ec: ExecutionContext): Source[(Page, WikiItem), _] = {
    val source =
      mode match {
        case Mode.Pages(raw) =>
          client
            .fetch(MediaWikiSelector.Pages(raw), Some(MediaWikiContent.Revisions))
            .via(Reporter.pageFlow(reporter))

        case Mode.From(initialPage) =>
          Source
            .future(findPagesToIgnore(client))
            .flatMapConcat(ignoredPages => findItemPages(ignoredPages, client, reporter))
            .dropWhile { case (page, _) => page.name != initialPage }

        case Mode.All =>
          Source
            .future(findPagesToIgnore(client))
            .flatMapConcat(ignoredPages => findItemPages(ignoredPages, client, reporter))
      }

    source
      .via(decodingFlow(reporter))
      .via(fetchImageFlow(client, reporter))
  }

  private def findPagesToIgnore(
    client: MediaWikiClient
  )(implicit materializer: Materializer): Future[Set[Page.ID]] =
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
    reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]
  ): Source[(Page, String), _] =
    client
      .fetch(
        MediaWikiSelector.PagesThatTransclude(Page.Name.Template(infobox)),
        Some(MediaWikiContent.Revisions)
      )
      .filterNot { case (page, _) => ignoredPages.contains(page.id) }
      .via(Reporter.pageFlow(reporter))

  private def decodingFlow(
    reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]
  ): Flow[(Page, String), (Page, WikiItem.Infobox), _] =
    Flow[(Page, String)]
      .map { case (page, content) => (page, TermParser.parse(content)) }
      .via(Reporter.pageFlow(reporter))
      .mapConcat { case (page, terms) => ItemPageDecoder.extractItemTemplates(terms).map((page, _)) }
      .via(Reporter.pageFlow(reporter))
      .map { case (page, (itemVersion, obj)) => (page, ItemInfoboxDecoder.decode(page, itemVersion, obj)) }
      .via(Reporter.pageFlow(reporter))

  private def fetchImageFlow(
    client: MediaWikiClient,
    reporter: ActorRef[Cache.Message.NewEntry[(Page, Throwable)]]
  )(implicit ec: ExecutionContext): Flow[(Page, WikiItem.Infobox), (Page, WikiItem), _] =
    Flow[(Page, WikiItem.Infobox)]
      .mapAsync(parallelism = 10) { case (page, infobox) =>
        fetchImages(infobox.imageBins, client).transform {
          case Failure(error) =>
            Success(Left((page, error)))
          case Success(images) =>
            Success(Right((page, WikiItem(infobox, images))))
        }
      }
      .via(Reporter.flow(reporter))

  private def fetchImages(
    wikiBins: NonEmptyList[(Item.Image.Bin, Page.Name.File)],
    client: MediaWikiClient
  )(implicit ec: ExecutionContext): Future[NonEmptyList[WikiItem.Image]] =
    Future.sequence(
      wikiBins.map { case (bin, fileName) =>
        client.fetchImage(fileName).map(data => WikiItem.Image(bin, fileName, data))
      }.toList
    ).map(bins => NonEmptyList.fromListUnsafe(bins))
}
