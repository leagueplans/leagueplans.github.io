package ddm.scraper.wiki.decoder.items

import ddm.scraper.wiki.decoder.{DecoderException, DecoderResult, SimpleInfoboxObjectExtractor}
import ddm.scraper.wiki.model.InfoboxVersion
import ddm.scraper.wiki.parser.Term
import ddm.scraper.wiki.parser.Term.Template

object ItemPageObjectExtractor {
  final case class VersionedObjects(
    version: InfoboxVersion,
    itemObject: Template.Object,
    maybeBonusesObject: Option[Template.Object],
  )

  private val bonusesExtractor = new SimpleInfoboxObjectExtractor("bonuses")

  def extract(terms: List[Term]): List[DecoderResult[VersionedObjects]] = {
    val itemExtractions = ItemInfoboxObjectExtractor.extract(terms)
    bonusesExtractor.extractIfExists(terms) match {
      case None =>
        itemExtractions.map(_.map { case (version, obj) => VersionedObjects(version, obj, None) })

      case Some(bonusesExtractions) =>
        val (_, bonusesObjects) = bonusesExtractions.partitionMap(identity)
        val (itemExtractionErrors, itemObjects) = itemExtractions.partitionMap(identity)
        itemExtractionErrors.map(Left(_)) ++ link(bonusesObjects, itemObjects)
    }
  }

  private def link(
    bonusesObjects: List[(InfoboxVersion, Template.Object)],
    itemObjects: List[(InfoboxVersion, Template.Object)]
  ): List[DecoderResult[VersionedObjects]] =
    itemObjects.map { case (itemVersion, itemObject) =>
      bonusesObjects.collectFirst {
        case (bonusesVersion, bonusesObject) if itemVersion.isSubVersionOf(bonusesVersion) =>
          VersionedObjects(itemVersion, itemObject, Some(bonusesObject))
      }.toRight(left = new DecoderException(
        s"Failed to pair item infobox version [${itemVersion.raw.mkString(", ")}] with a bonuses infobox version"
      ))
    }
}
