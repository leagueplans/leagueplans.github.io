package ddm.scraper.wiki.scraper

import cats.data.NonEmptyList
import ddm.common.model.Item
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.decoder.items.{ItemPageDecoder, ItemPageObjectExtractor}
import ddm.scraper.wiki.http.{WikiClient, WikiContentType, WikiSelector}
import ddm.scraper.wiki.model.{Page, PageDescriptor, WikiItem}
import ddm.scraper.wiki.parser.TermParser
import ddm.scraper.wiki.streaming.*
import zio.stream.ZStream
import zio.{Chunk, Task, Trace, UIO, ZIO}

import scala.util.{Success, Try}

object ItemsScraper {
  def make(args: CommandLineArgs, client: WikiClient): Try[ItemsScraper] =
    parseMode(args).map(ItemsScraper(client, _))

  private def parseMode(args: CommandLineArgs): Try[ItemsScraper.Mode] =
    args.getOpt("pages") { input =>
      val pages = input.split('|').map[PageDescriptor.Name.Other](PageDescriptor.Name.Other.apply)
      Success(ItemsScraper.Mode.Pages(pages.toVector))
    }.map(_.getOrElse(ItemsScraper.Mode.All))

  private val infoboxSelector = WikiSelector.PagesThatTransclude(PageDescriptor.Name.Template("Infobox Item"))
  private val ignoredCategories: Set[PageDescriptor.Name.Category] =
    Set(
      "Inaccessible items",
      "Needs examine added"
    ).map(PageDescriptor.Name.Category.apply)

  enum Mode {
    case Pages(raw: Vector[PageDescriptor.Name.Other])
    case All
  }
}

final class ItemsScraper(client: WikiClient, mode: ItemsScraper.Mode) {
  def scrape(using Trace): PageStream[WikiItem] = {
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
      .pageMapZIOPar(n = 8)(infoboxes =>
        fetchImages(infoboxes.item.imageBins).map(WikiItem(infoboxes, _))
      )
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

  private def fetchImages(
    wikiBins: NonEmptyList[(Item.Image.Bin, PageDescriptor.Name.File)]
  )(using Trace): Task[NonEmptyList[WikiItem.Image]] =
    ZIO.foreachPar(wikiBins.toList)((bin, fileName) =>
      client
        .fetchImage(fileName)
        .map(data => WikiItem.Image(bin, fileName, data))
    ).map(NonEmptyList.fromListUnsafe)
}
