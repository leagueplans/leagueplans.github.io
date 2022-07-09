package ddm.scraper.main.runner

import akka.actor.typed.{ActorRef, ActorSystem}
import ddm.common.model.Item
import ddm.scraper.dumper.{Cache, CachingWriter, ItemDumper}
import ddm.scraper.main.CommandLineArgs
import ddm.scraper.wiki.http.MediaWikiClient
import ddm.scraper.wiki.model.{Page, WikiItem}
import ddm.scraper.wiki.scraper.ItemScraper
import io.circe.parser.decode

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.util.chaining.scalaUtilChainingOps

object ScrapeItemsRunner {
  def from(args: CommandLineArgs): ScrapeItemsRunner = {
    val dumpDirectory = args.get("target-directory")(Path.of(_).resolve("dump"))

    new ScrapeItemsRunner(
      parseMode(args),
      idMapFile = args.get("id-map")(Path.of(_)),
      itemsFile = dumpDirectory.resolve("data/items.json"),
      imagesDirectory = dumpDirectory.resolve("images/items")
    )
  }

  private def parseMode(args: CommandLineArgs): ItemScraper.Mode =
    args
      .getOpt("pages")(_.split('|').map(Page.Name.Other).toList.pipe(ItemScraper.Mode.Pages))
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
    spawnIDMapWriter: Spawn[Cache.Message[((Page.ID, WikiItem.Version), Item.ID)]],
    spawnItemWriter: Spawn[Cache.Message[Item]]
  )(implicit system: ActorSystem[_]): Unit = {
    import system.executionContext
    val idMapWriterBehavior = CachingWriter.to[((Page.ID, WikiItem.Version), Item.ID)](idMapFile)
    val itemWriterBehavior = CachingWriter.to[Item](itemsFile, StandardOpenOption.CREATE_NEW)

    ItemScraper
      .scrape(mode, client, reporter)
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

  private def loadIDMap(): Map[(Page.ID, WikiItem.Version), Item.ID] =
    if (Files.exists(idMapFile)) {
      decode[List[((Page.ID, WikiItem.Version), Item.ID)]](Files.readString(idMapFile))
        .toTry.get
        .toMap
    } else
      Map.empty
}
