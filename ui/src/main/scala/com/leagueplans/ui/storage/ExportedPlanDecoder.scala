package com.leagueplans.ui.storage

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step, StepDetails}
import com.leagueplans.ui.storage.migrations.{MigrationError, Migrator}
import com.leagueplans.ui.storage.model.{PlanExport, PlanMetadata, StepMappings}
import com.raquo.airstream.core.EventStream

object ExportedPlanDecoder {
  private type ForestBuilder =
    (Map[Step.ID, Step], Map[Step.ID, List[Step.ID]], List[Step.ID]) => Either[String, Forest[Step.ID, Step]]

  /** For plans we've stored ourselves. Only faults that would hang the app are rejected, so
    * that users aren't locked out of plans that open today. */
  def decode(input: PlanExport): EventStream[Either[DecodingFailure | MigrationError, (PlanMetadata, Plan)]] =
    decode(input, Forest.acyclic)

  /** For plan files provided by users, which must describe a well-formed forest */
  def decodeImport(input: PlanExport): EventStream[Either[DecodingFailure | MigrationError, (PlanMetadata, Plan)]] =
    decode(input, Forest.validated)

  private def decode(
    input: PlanExport,
    toForest: ForestBuilder
  ): EventStream[Either[DecodingFailure | MigrationError, (PlanMetadata, Plan)]] =
    Migrator.run(input).map(maybeData =>
      for {
        data <- maybeData
        metadata <- Decoder.decode[PlanMetadata](data.metadata)
        steps <- decodeSteps(data.steps)
        mappings <- Decoder.decode[StepMappings](data.mappings)
        settings <- Decoder.decode[Plan.Settings](data.settings)
        forest <- toForest(steps, mappings.toChildren, mappings.roots).left.map(reason =>
          DecodingFailure(s"Invalid plan structure: $reason")
        )
      } yield (metadata, Plan(metadata.name, forest, settings))
    )

  private def decodeSteps(
    encodedSteps: Map[Step.ID, Encoding]
  ): Either[DecodingFailure, Map[Step.ID, Step]] = {
    var decodingFailures = 0

    val steps = 
      encodedSteps.foldLeft(Map.empty[Step.ID, StepDetails]) { case (acc, (id, encoding)) =>
        Decoder.decode[StepDetails](encoding) match {
          case Left(_) => 
            decodingFailures += 1
            acc
            
          case Right(step) => 
            acc + (id -> step)
        }
      }
      
    Either.cond(
      decodingFailures == 0,
      steps.map((id, details) => (id, Step(id, details))),
      DecodingFailure(s"Failed to decode $decodingFailures steps")
    )
  }
}
