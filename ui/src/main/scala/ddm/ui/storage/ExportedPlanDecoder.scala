package ddm.ui.storage

import ddm.codec.Encoding
import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Plan, Step, StepDetails}
import ddm.ui.storage.model.{PlanExport, PlanMetadata, StepMappings}

object ExportedPlanDecoder {
  def decode(data: PlanExport): Either[DecodingFailure, (PlanMetadata, Plan)] =
    for {
      metadata <- Decoder.decode[PlanMetadata](data.metadata)
      steps <- decodeSteps(data.steps)
      mappings <- Decoder.decode[StepMappings](data.mappings)
      settings <- Decoder.decode[Plan.Settings](data.settings)
    } yield (metadata, Plan(Forest.from(steps, mappings.value), settings))
  
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
