package ddm.scraper.wiki.scraper

import akka.actor.typed.ActorRef
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink, Source}
import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.reporter.Reporter
import ddm.scraper.wiki.decoder.{ItemInfoboxDecoder, RichTemplateObject, RichTerms}
import ddm.scraper.wiki.http.{MediaWikiClient, MediaWikiContent, MediaWikiSelector}
import ddm.scraper.wiki.model.{Page, WikiItem}
import ddm.scraper.wiki.parser.Term.Template
import ddm.scraper.wiki.parser.{Term, TermParser}

import scala.annotation.tailrec
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object ItemScraper {
  private val infobox = "Infobox Item"
  private val ignoredCategories: Set[Page.Name.Category] =
    Set("Inaccessible items").map(Page.Name.Category)

  def scrapeAll(
    client: MediaWikiClient,
    reporter: ActorRef[Reporter.Message.Failure]
  )(implicit materializer: Materializer, ec: ExecutionContext): Source[(Page, WikiItem), _] =
    Source
      .future(findPagesToIgnore(client))
      .flatMapConcat(ignoredPages => findItemPages(ignoredPages, client, reporter))
      .via(decodingFlow(reporter))
      .via(fetchImageFlow(client, reporter))

  /** Inclusive of the named page */
  def scrapeFrom(
    initialPage: Page.Name.Other,
    client: MediaWikiClient,
    reporter: ActorRef[Reporter.Message.Failure]
  )(implicit materializer: Materializer, ec: ExecutionContext): Source[(Page, WikiItem), _] =
    Source
      .future(findPagesToIgnore(client))
      .flatMapConcat(ignoredPages => findItemPages(ignoredPages, client, reporter))
      .dropWhile { case (page, _) => page.name != initialPage }
      .via(decodingFlow(reporter))
      .via(fetchImageFlow(client, reporter))

  def scrape(
    pages: List[Page.Name],
    client: MediaWikiClient,
    reporter: ActorRef[Reporter.Message.Failure]
  )(implicit materializer: Materializer, ec: ExecutionContext): Source[(Page, WikiItem), _] =
    client
      .fetch(MediaWikiSelector.Pages(pages), Some(MediaWikiContent.Revisions))
      .via(Reporter.pageFlow(reporter))
      .via(decodingFlow(reporter))
      .via(fetchImageFlow(client, reporter))

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
    reporter: ActorRef[Reporter.Message.Failure]
  ): Source[(Page, String), _] =
    client
      .fetch(
        MediaWikiSelector.PagesThatTransclude(Page.Name.Template(infobox)),
        Some(MediaWikiContent.Revisions)
      )
      .filterNot { case (page, _) => ignoredPages.contains(page.id) }
      .via(Reporter.pageFlow(reporter))

  private def decodingFlow(
    reporter: ActorRef[Reporter.Message.Failure]
  ): Flow[(Page, String), (Page, WikiItem.Infobox), _] =
    Flow[(Page, String)]
      .mapConcat { case (page, content) => splitByItem(content).map((page, _)) }
      .via(Reporter.pageFlow(reporter))
      .map { case (page, obj) => (page, ItemInfoboxDecoder.decode(page, obj)) }
      .via(Reporter.pageFlow(reporter))

  private def splitByItem(content: String): List[Either[Throwable, Template.Object]] =
    TermParser.parse(content) match {
      case Left(error) => List(Left(error))
      case Right(terms) => extractItemTemplates(terms)
    }

  private val switchInfoboxes =
    Set("switch infobox", "multi infobox")

  private def extractItemTemplates(terms: List[Term]): List[Either[Throwable, Template.Object]] =
    terms
      .collectFirst {
        case template: Template if template.name.toLowerCase == infobox.toLowerCase =>
          template.objects.toList.map(Right(_))

        case template: Template if switchInfoboxes.contains(template.name.toLowerCase) =>
          unwrapSwitchBoxes(template.objects.toList, acc = List.empty)
      }
      .getOrElse(List(Left(new RuntimeException("No infobox found"))))

  @tailrec
  private def unwrapSwitchBoxes(
    remaining: List[Template.Object],
    acc: List[Either[Throwable, Template.Object]]
  ): List[Either[Throwable, Template.Object]] =
    remaining match {
      case Nil => acc

      case head :: tail =>
        head.decode("item")(
          _.collect { case template: Template => template }.as[Template]
        ) match {
          case Left(error) =>
            unwrapSwitchBoxes(
              remaining = tail,
              acc = acc :+ Left(error)
            )

          case Right(template) if template.name.toLowerCase == infobox.toLowerCase =>
            unwrapSwitchBoxes(
              remaining = tail,
              acc = acc ++ template.objects.toList.map(Right(_))
            )

          case Right(template) if switchInfoboxes.contains(template.name.toLowerCase) =>
            unwrapSwitchBoxes(
              remaining = tail ++ template.objects,
              acc = acc
            )

          case Right(_) =>
            unwrapSwitchBoxes(remaining = tail, acc = acc)
        }
    }

  private def fetchImageFlow(
    client: MediaWikiClient,
    reporter: ActorRef[Reporter.Message.Failure]
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
