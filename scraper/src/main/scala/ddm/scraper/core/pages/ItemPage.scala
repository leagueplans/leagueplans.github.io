package ddm.scraper.core.pages

import ddm.scraper.core.WikiBrowser
import ddm.scraper.scrapers.items.Item
import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.model.Element
import org.log4s.{Logger, getLogger}

final class ItemPage[B <: Browser](wikiBrowser: WikiBrowser[B], wikiPath: String) {
  private val logger: Logger = getLogger

  def fetchItem(): Option[Item] = {
    val infobox = wikiBrowser.fetchHtml(wikiPath) >> element(".infobox-item")
    val name = (infobox >> element(".infobox-header")).text
    val filePath = (infobox >> element(".inventory-image") >> elementList(".image")).last.attr("href")
    val (_, image) = new FilePage(wikiBrowser, filePath).fetchImage()

    val maybeItemId = findRow(infobox, "Item ID")
    val maybeStackable = findRow(infobox, "Stackable")
    val maybeExamine = findRow(infobox, "Examine")

    (for {
      itemId <- maybeItemId
      rawStackable <- maybeStackable
      stackable <- parseStackable(rawStackable)
      examine <- maybeExamine
    } yield {
      Item(
        itemId,
        name,
        stackable,
        examine,
        image
      )
    }).orElse {
      logger.error(
        s"Failed to parse item: [$name], [ID = $maybeItemId]," +
          s" [stackable = $maybeStackable], [examine = $maybeExamine]"
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
}
