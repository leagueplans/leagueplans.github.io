package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder

/** Rewrites the item IDs held by a step's effects, requirements and repetitions.
  *
  * Works on those fields rather than on whole steps, since the shape of a step has changed
  * between schema versions while these have not. Each migration decodes its own version's
  * step and hands the fields over. Repetitions only hold item IDs from V11 onwards.
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

  /** A stop condition on a removed item is dropped, as a requirement is. A step left with no
    * condition at all repeats once.
    */
  def repetitions(repetitions: Encoding): MigrationResult[Encoding] =
    decodeOrdinal(repetitions).flatMap {
      case (ordinal @ 1, until) =>
        for {
          (condition, max) <- until.as[(Encoding, Option[Encoding])]
          updatedCondition <- migrateStopCondition(condition)
        } yield updatedCondition match {
          case Some(c) => encodeCoproduct(ordinal, (c, max))
          // A fixed count is a signed int, unlike the item IDs this class is set up for
          case None => encodeCoproduct(0, Tuple1(Encoder.encode(1)(using Encoder.intEncoder)))
        }

      case _ =>
        Right(repetitions)
    }

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

  private def migrateStopCondition(condition: Encoding): MigrationResult[Option[Encoding]] =
    decodeOrdinal(condition).flatMap {
      case (ordinal @ 1, itemCount) =>
        itemCount.as[(Int, Encoding, Encoding, Encoding, Encoding, Encoding)].map(
          (id, where, forms, comparison, quantity, stopBefore) =>
            migrateID(id).map(newID =>
              encodeCoproduct(ordinal, Encoder.encode((newID, where, forms, comparison, quantity, stopBefore)))
            )
        )

      case (ordinal @ (3 | 4), andOr) =>
        for {
          (left, right) <- andOr.as[(Encoding, Encoding)]
          updatedLeft <- migrateStopCondition(left)
          updatedRight <- migrateStopCondition(right)
        } yield (updatedLeft, updatedRight) match {
          case (Some(l), Some(r)) => Some(encodeCoproduct(ordinal, Encoder.encode((l, r))))
          case (onlyOne, None) => onlyOne
          case (None, onlyOne) => onlyOne
        }

      case _ =>
        Right(Some(condition))
    }
}
