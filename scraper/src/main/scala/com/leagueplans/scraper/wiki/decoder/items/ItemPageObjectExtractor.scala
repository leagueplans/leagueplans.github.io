package com.leagueplans.scraper.wiki.decoder.items

import com.leagueplans.scraper.telemetry.WithAnnotation
import com.leagueplans.scraper.wiki.decoder.TermOps.*
import com.leagueplans.scraper.wiki.decoder.{DecoderResult, SimpleInfoboxObjectExtractor}
import com.leagueplans.scraper.wiki.model.InfoboxVersion
import com.leagueplans.scraper.wiki.parser.Term
import com.leagueplans.scraper.wiki.parser.Term.Template
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
  )(using Trace): UIO[List[VersionedObjects]] = {
    lazy val maybeSharedBonuses = sharedBonuses(bonusesObjects)

    ZIO.foreach(itemObjects) { (itemVersion, itemObject) =>
      val unequipable = isUnequipable(itemObject)
      val maybeBonuses =
        if (unequipable)
          None
        else
          bonusesObjects
            .collectFirst {
              case (bonusesVersion, bonusesObject) if itemVersion.isSubVersionOf(bonusesVersion) =>
                bonusesObject
            }
            .orElse(maybeSharedBonuses)

      val log = if (maybeBonuses.isEmpty && !unequipable)
        WithAnnotation.forLogs("item-infobox-version" -> itemVersion.raw.mkString(", "))(
          ZIO.logWarning("Failed to pair item infobox version with a bonuses infobox version")
        )
      else
        ZIO.unit

      log.as(VersionedObjects(itemVersion, itemObject, maybeBonuses))
    }
  }

  /** The item infobox says, per version, whether the item can be equipped. Broken and
    * mangled variants say no, and have no bonuses of their own to pair with. A missing or
    * unreadable flag decides nothing, and pairing goes ahead as usual.
    */
  private def isUnequipable(itemObject: Template.Object): Boolean =
    itemObject.decodeOpt("equipable")(_.asBoolean) == Right(Some(false))

  /** A bonuses infobox to fall back on when an item version pairs with none by name.
    *
    * Version names don't always line up between the two infoboxes - an unversioned item
    * with versioned bonuses, or "Charged" against "Active" - but when every bonuses version
    * decodes to the same thing, it doesn't matter which one an item version gets.
    */
  private def sharedBonuses(bonusesObjects: List[(InfoboxVersion, Template.Object)]): Option[Template.Object] =
    bonusesObjects.map((_, obj) => BonusesInfoboxDecoder.decode(obj)).distinct match {
      case List(Right(_)) => bonusesObjects.headOption.map((_, obj) => obj)
      case _ => None
    }
}
