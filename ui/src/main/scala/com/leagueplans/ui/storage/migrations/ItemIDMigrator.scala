package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step

private[migrations] object ItemIDMigrator {
  def apply(
    oldToNewIDs: Map[Int, Int],
    steps: Map[Step.ID, Encoding]
  ): MigrationResult[Map[Step.ID, Encoding]] = {
    val migrateEffect = migrateCoproduct(generateEffectMigrations(oldToNewIDs))
    migrateList(steps.toList)((id, details) =>
      migrateDetails(details, oldToNewIDs, migrateEffect).map((id, _))
    ).map(_.toMap)
  }
    
  private def migrateDetails(
    details: Encoding,
    oldToNewIDs: Map[Int, Int],
    migrateEffect: Migration,
  ): MigrationResult[Encoding] =
    for {
      (description, effects, requirements) <- details.as[(Encoding, List[Encoding], List[Encoding])]
      updatedEffects <- migrateList(effects)(migrateEffect)
      updatedRequirements <- migrateList(requirements)(migrateRequirement(oldToNewIDs, _))
    } yield Encoder.encode((description, updatedEffects, updatedRequirements))

  private def generateEffectMigrations(oldToNewIDs: Map[Int, Int]): Map[Int, Migration] = {
    // Item IDs are stored as unsigned ints
    given Decoder[Int] = Decoder.unsignedIntDecoder
    given Encoder[Int] = Encoder.unsignedIntEncoder
    Map(
      1 -> (addItem =>
        addItem.as[(Int, Encoding, Encoding, Encoding)].map((id, quantity, target, note) =>
          Encoder.encode((oldToNewIDs.getOrElse(id, id), quantity, target, note))
        )
      ),
      2 -> (moveItem =>
        moveItem.as[(Int, Encoding, Encoding, Encoding, Encoding, Encoding)].map(
          (id, quantity, source, notedInSource, target, noteInTarget) =>
            Encoder.encode((oldToNewIDs.getOrElse(id, id), quantity, source, notedInSource, target, noteInTarget))
        )
      )
    )
  }

  private def migrateRequirement(
    oldToNewIDs: Map[Int, Int],
    requirement: Encoding
  ): MigrationResult[Encoding] =
    decodeOrdinal(requirement).flatMap {
      case (1, tool) =>
        val encodedOrdinal = Encoder.encode(1)(using Encoder.unsignedIntEncoder)
        // Item IDs are stored as unsigned ints
        given Decoder[Int] = Decoder.unsignedIntDecoder
        given Encoder[Int] = Encoder.unsignedIntEncoder
        tool.as[(Int, Encoding)].map((id, location) =>
          Encoder.encode((encodedOrdinal, (oldToNewIDs.getOrElse(id, id), location)))
        )
        
      case (ordinal @ (2 | 3), andOr) =>
        val encodedOrdinal = Encoder.encode(ordinal)(using Encoder.unsignedIntEncoder)
        for {
          (left, right) <- andOr.as[(Encoding, Encoding)]
          updatedLeft <- migrateRequirement(oldToNewIDs, left)
          updatedRight <- migrateRequirement(oldToNewIDs, right)
        } yield Encoder.encode((encodedOrdinal, (updatedLeft, updatedRight)))
        
      case _ => 
        Right(requirement)
    }
}
