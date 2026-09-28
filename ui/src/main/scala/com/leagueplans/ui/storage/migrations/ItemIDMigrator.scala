package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

/** Rewrites the item IDs held by a step's effects and requirements.
  *
  * Works on the effect and requirement lists rather than on whole steps, since the shape
  * of a step has changed between schema versions while these two have not. Each migration
  * decodes its own version's step and hands the lists over.
  *
  * @param oldToNewIDs items folded into another, whose references move to the survivor
  * @param removedIDs items that no longer exist, whose references are dropped. The app
  *                   cannot show an item it has no data for, so a reference left behind
  *                   would break the plan. The step itself, and its description, remain.
  */
private[migrations] final class ItemIDMigrator(
  oldToNewIDs: Map[Int, Int],
  removedIDs: Set[Int]
) {
  // Item IDs are stored as unsigned ints
  private given Decoder[Int] = Decoder.unsignedIntDecoder
  private given Encoder[Int] = Encoder.unsignedIntEncoder

  def effects(effects: List[Encoding]): MigrationResult[List[Encoding]] =
    migrateList(effects)(migrateEffect).map(_.flatten)

  def requirements(requirements: List[Encoding]): MigrationResult[List[Encoding]] =
    migrateList(requirements)(migrateRequirement).map(_.flatten)

  private def migrateID(id: Int): Option[Int] =
    Option.unless(removedIDs.contains(id))(oldToNewIDs.getOrElse(id, id))

  private def migrateEffect(effect: Encoding): MigrationResult[Option[Encoding]] =
    decodeOrdinal(effect).flatMap {
      case (ordinal @ 1, addItem) =>
        addItem.as[(Int, Encoding, Encoding, Encoding)].map((id, quantity, target, note) =>
          migrateID(id).map(newID => encodeCoproduct(ordinal, Encoder.encode((newID, quantity, target, note))))
        )

      case (ordinal @ 2, moveItem) =>
        moveItem.as[(Int, Encoding, Encoding, Encoding, Encoding, Encoding)].map(
          (id, quantity, source, notedInSource, target, noteInTarget) =>
            migrateID(id).map(newID =>
              encodeCoproduct(
                ordinal,
                Encoder.encode((newID, quantity, source, notedInSource, target, noteInTarget))
              )
            )
        )

      case _ =>
        Right(Some(effect))
    }

  /** A requirement for a removed item is dropped. One half of an `And` or `Or` being
    * dropped leaves the other half standing alone, which is the requirement the step still
    * meaningfully has.
    */
  private def migrateRequirement(requirement: Encoding): MigrationResult[Option[Encoding]] =
    decodeOrdinal(requirement).flatMap {
      case (ordinal @ 1, tool) =>
        tool.as[(Int, Encoding)].map((id, location) =>
          migrateID(id).map(newID => encodeCoproduct(ordinal, Encoder.encode((newID, location))))
        )

      case (ordinal @ (2 | 3), andOr) =>
        for {
          (left, right) <- andOr.as[(Encoding, Encoding)]
          updatedLeft <- migrateRequirement(left)
          updatedRight <- migrateRequirement(right)
        } yield (updatedLeft, updatedRight) match {
          case (Some(l), Some(r)) => Some(encodeCoproduct(ordinal, Encoder.encode((l, r))))
          case (onlyOne, None) => onlyOne
          case (None, onlyOne) => onlyOne
        }

      case _ =>
        Right(Some(requirement))
    }
}
