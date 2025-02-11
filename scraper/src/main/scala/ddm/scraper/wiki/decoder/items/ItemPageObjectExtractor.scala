package ddm.scraper.wiki.decoder.items

import ddm.scraper.telemetry.WithAnnotation
import ddm.scraper.wiki.decoder.{DecoderResult, SimpleInfoboxObjectExtractor}
import ddm.scraper.wiki.model.InfoboxVersion
import ddm.scraper.wiki.parser.Term
import ddm.scraper.wiki.parser.Term.Template
import zio.{Trace, UIO, ZIO}

object ItemPageObjectExtractor {
  final case class VersionedObjects(
    version: InfoboxVersion,
    itemObject: Template.Object,
    maybeBonusesObject: Option[Template.Object],
  )

  private val bonusesExtractor = SimpleInfoboxObjectExtractor("bonuses")

  def extract(terms: List[Term])(using Trace): UIO[List[DecoderResult[VersionedObjects]]] = {
    val itemExtractions = ItemInfoboxObjectExtractor.extract(terms)
    bonusesExtractor.extractIfExists(terms) match {
      case None =>
        ZIO.succeed(
          itemExtractions.map(_.map((version, obj) => VersionedObjects(version, obj, None)))
        )

      case Some(bonusesExtractions) =>
        val (_, bonusesObjects) = bonusesExtractions.partitionMap(identity)
        val (itemExtractionErrors, itemObjects) = itemExtractions.partitionMap(identity)
        link(bonusesObjects, itemObjects).map(versionedObjects =>
          itemExtractionErrors.map(Left(_)) ++ versionedObjects.map(Right(_))
        )
    }
  }

  private def link(
    bonusesObjects: List[(InfoboxVersion, Template.Object)],
    itemObjects: List[(InfoboxVersion, Template.Object)]
  )(using Trace): UIO[List[VersionedObjects]] =
    ZIO.foreach(itemObjects) { (itemVersion, itemObject) =>
      val maybeBonuses = bonusesObjects.collectFirst {
        case (bonusesVersion, bonusesObject) if itemVersion.isSubVersionOf(bonusesVersion) =>
          bonusesObject
      }

      val log = if (maybeBonuses.isEmpty)
        WithAnnotation.forLogs("item-infobox-version" -> itemVersion.raw.mkString(", "))(
          ZIO.logWarning("Failed to pair item infobox version with a bonuses infobox version")
        )
      else
        ZIO.unit

      log.as(VersionedObjects(itemVersion, itemObject, maybeBonuses))
    }
}
