package ddm.scraper.wiki.pages

import ddm.common.model.Item
import ddm.scraper.wiki.WikiBrowser
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.log4s.{Logger, getLogger}

final class ItemPage[B <: Browser](wikiBrowser: WikiBrowser[B], wikiPath: String) {
  private val logger: Logger = getLogger

  def fetchItemAndImage(): Option[(Item, Array[Byte])] = {
    val infobox = wikiBrowser.fetchHtml(wikiPath) >> element(".infobox-item")
    val name = (infobox >> element(".infobox-header")).text

    val maybeFilePath = (infobox >> element(".inventory-image") >> elementList(".image")).lastOption.map(_.attr("href"))
    val maybeImageNameAndImage = maybeFilePath.map(new FilePage(wikiBrowser, _).fetchImage())

    val maybeItemId = findRow(infobox, "Item ID")
    val maybeStackable = findRow(infobox, "Stackable")
    val maybeExamine = findRow(infobox, "Examine")

    (for {
      (_, image) <- maybeImageNameAndImage
      itemId <- maybeItemId
      rawStackable <- maybeStackable
      stackable <- parseStackable(rawStackable)
      examine <- maybeExamine
    } yield (Item(parseItemId(itemId), name, stackable, examine), image)).orElse {
      logger.error(
        s"Failed to parse item: [$name], [ID = $maybeItemId]," +
          s" [stackable = $maybeStackable], [examine = $maybeExamine], [image = ${maybeImageNameAndImage.isDefined}]"
      )
      None
    }
  }

  private def findRow(infobox: Element, key: String): Option[String] =
    (infobox >> elementList("tr"))
      .find(row =>
        (row >?> element("th"))
          .map(header => (header >?> element("a")).getOrElse(header))
          .exists(_.text == key)
      )
      .map(_ >> element("td"))
      .map(_.text)

  private def parseStackable(raw: String): Option[Boolean] =
    raw match {
      case "Yes" => Some(true)
      case "No" => Some(false)
      case _ => None
    }

  private def parseItemId(raw: String): Item.ID =
    Item.ID(raw.takeWhile(_ != ','))
}
