package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step

import scala.annotation.tailrec

private[migrations] object ItemIDMigrator {
  def apply(
    oldToNewIDs: Map[Int, Int],
    steps: Map[Step.ID, Encoding]
  ): Either[DecodingFailure | MigrationError, Map[Step.ID, Encoding]] = 
    migrateHelper(oldToNewIDs, steps.toList)(
      migrateCoproduct(generateEffectMigrations(oldToNewIDs))
    )
  
  @tailrec
  private def migrateHelper(
    oldToNewIDs: Map[Int, Int],
    steps: List[(Step.ID, Encoding)],
    acc: List[(Step.ID, Encoding)] = List.empty
  )(migrateEffect: Migration): Either[DecodingFailure | MigrationError, Map[Step.ID, Encoding]] =
    steps match {
      case Nil => Right(acc.toMap)
      case (id, details) :: t =>
        migrateStep(details, oldToNewIDs, migrateEffect) match {
          case Left(error) => 
            Left(error)
          case Right(updatedDetails) =>
            migrateHelper(oldToNewIDs, t, acc :+ (id, updatedDetails))(migrateEffect)
        }
    }
    
  private def migrateStep(
    details: Encoding,
    oldToNewIDs: Map[Int, Int],
    migrateEffect: Migration,
  ): Either[DecodingFailure | MigrationError, Encoding] =
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
  ): Either[DecodingFailure | MigrationError, Encoding] =
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
