package ddm.scraper.wiki.decoder.items

import ddm.scraper.wiki.decoder.{DecoderResult, SimpleInfoboxObjectExtractor}
import ddm.scraper.wiki.model.InfoboxVersion
import ddm.scraper.wiki.parser.Term
import ddm.scraper.wiki.parser.Term.Template
import org.log4s.{Logger, getLogger}

object ItemPageObjectExtractor {
  private val logger: Logger = getLogger

  final case class VersionedObjects(
    version: InfoboxVersion,
    itemObject: Template.Object,
    maybeBonusesObject: Option[Template.Object],
  )

  private val bonusesExtractor = SimpleInfoboxObjectExtractor("bonuses")

  def extract(terms: List[Term]): List[DecoderResult[VersionedObjects]] = {
    val itemExtractions = ItemInfoboxObjectExtractor.extract(terms)
    bonusesExtractor.extractIfExists(terms) match {
      case None =>
        itemExtractions.map(_.map((version, obj) => VersionedObjects(version, obj, None)))

      case Some(bonusesExtractions) =>
        val (_, bonusesObjects) = bonusesExtractions.partitionMap(identity)
        val (itemExtractionErrors, itemObjects) = itemExtractions.partitionMap(identity)
        itemExtractionErrors.map(Left(_)) ++ link(bonusesObjects, itemObjects).map(Right(_))
    }
  }

  private def link(
    bonusesObjects: List[(InfoboxVersion, Template.Object)],
    itemObjects: List[(InfoboxVersion, Template.Object)]
  ): List[VersionedObjects] =
    itemObjects.map { (itemVersion, itemObject) =>
      val maybeBonuses = bonusesObjects.collectFirst {
        case (bonusesVersion, bonusesObject) if itemVersion.isSubVersionOf(bonusesVersion) =>
          bonusesObject
      }

      if (maybeBonuses.isEmpty)
        logger.warn(s"Failed to pair item infobox version [${itemVersion.raw.mkString(", ")}] with a bonuses infobox version")

      VersionedObjects(itemVersion, itemObject, maybeBonuses)
    }
}
