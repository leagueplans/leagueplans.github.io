package ddm.scraper.main.runner

import akka.actor.typed.{ActorRef, ActorSystem}
import ddm.common.model.Item
import ddm.scraper.dumper.{Cache, CachingWriter, ItemDumper}
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.MediaWikiClient
import ddm.scraper.wiki.model.{InfoboxVersion, Page}
import ddm.scraper.wiki.scraper.ItemScraper
import io.circe.parser.decode

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.chaining.scalaUtilChainingOps

object ScrapeItemsRunner {
  def from(args: CommandLineArgs): ScrapeItemsRunner = {
    val dumpDirectory = args.get("target-directory")(Path.of(_).resolve("dump"))

    ScrapeItemsRunner(
      parseMode(args),
      idMapFile = args.get("id-map")(Path.of(_)),
      itemsFile = dumpDirectory.resolve("data/items.json"),
      imagesDirectory = dumpDirectory.resolve("dynamic/assets/images/items")
    )
  }

  private def parseMode(args: CommandLineArgs): ItemScraper.Mode =
    args
      .getOpt("pages")(
        _.split('|')
          .map[Page.Name.Other](Page.Name.Other.apply)
          .toList
          .pipe(ItemScraper.Mode.Pages.apply)
      )
      .orElse(args.getOpt("from")(name => ItemScraper.Mode.From(Page.Name.Other(name))))
      .getOrElse(ItemScraper.Mode.All)
}

final class ScrapeItemsRunner(
  mode: ItemScraper.Mode,
  idMapFile: Path,
  itemsFile: Path,
  imagesDirectory: Path
) extends Runner {
  def run(
    client: MediaWikiClient,
    reporter: ActorRef[Cache.Message[(Page, Throwable)]],
    spawnIDMapWriter: Spawn[Cache.Message[((Page.ID, InfoboxVersion), Item.ID)]],
    spawnItemWriter: Spawn[Cache.Message[Item]]
  )(using system: ActorSystem[?]): Unit = {
    import system.executionContext
    val idMapWriterBehavior = CachingWriter.to[((Page.ID, InfoboxVersion), Item.ID)](idMapFile)
    val itemWriterBehavior = CachingWriter.to[Item](itemsFile, StandardOpenOption.CREATE_NEW)

    ItemScraper
      .scrape(mode, client, (page, error) => reporter ! Cache.Message.NewEntry((page, error)))
      .runWith(
        ItemDumper.dump(
          loadIDMap(),
          imagesDirectory,
          spawnIDMapWriter(idMapWriterBehavior),
          spawnItemWriter(itemWriterBehavior)
        )
      )
      .onComplete(runStatus => reporter ! Cache.Message.Complete(runStatus))
  }

  private def loadIDMap(): Map[(Page.ID, InfoboxVersion), Item.ID] =
    if (Files.exists(idMapFile)) {
      decode[List[((Page.ID, InfoboxVersion), Item.ID)]](Files.readString(idMapFile))
        .toTry.get
        .toMap
    } else
      Map.empty
}
