package com.leagueplans.ui.storage

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step, StepDetails}
import com.leagueplans.ui.storage.migrations.{MigrationError, Migrator}
import com.leagueplans.ui.storage.model.{PlanExport, PlanMetadata, StepMappings}
import com.raquo.airstream.core.EventStream

object ExportedPlanDecoder {
  def decode(input: PlanExport): EventStream[Either[DecodingFailure | MigrationError, (PlanMetadata, Plan)]] =
    Migrator.run(input).map(maybeData =>
      for {
        data <- maybeData
        metadata <- Decoder.decode[PlanMetadata](data.metadata)
        steps <- decodeSteps(data.steps)
        mappings <- Decoder.decode[StepMappings](data.mappings)
        settings <- Decoder.decode[Plan.Settings](data.settings)
      } yield (
        metadata,
        Plan(metadata.name, Forest.from(steps, mappings.toChildren, mappings.roots), settings)
      )
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
