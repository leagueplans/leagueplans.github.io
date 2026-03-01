package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

object V5PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V4
  val toVersion: SchemaVersion = SchemaVersion.V5

  def apply(plan: PlanExport): Either[DecodingFailure | MigrationError, PlanExport] =
    for {
      (name, timestamp, schemaVersion) <- plan.metadata.as[(Encoding, Encoding, SchemaVersion)]
      _ <- validateInputVersion(schemaVersion)
      updatedSteps <- migrateSteps(plan.steps)
    } yield plan.copy(
      metadata = Encoder.encode((name, timestamp, toVersion)),
      steps = updatedSteps.toMap
    )

  private def migrateSteps(steps: Map[Step.ID, Encoding]): MigrationResult[List[(Step.ID, Encoding)]] =
    migrateList(steps.toList)((id, details) =>
      migrateDetails(details).map((id, _))
    )

  private def migrateDetails(details: Encoding): MigrationResult[Encoding] =
    details
      .as[(Encoding, List[Encoding], List[Encoding])]
      .map((description, effects, requirements) =>
        Encoder.encode(
          (description, effects, requirements, defaultRepetitions, defaultDuration)
        )
      )

  private val defaultRepetitions: Encoding =
    Encoder.encode(0)

  private val defaultDuration: Encoding =
    Encoder.encode((
      0, // Length
      encodeCoproduct(ordinal = /* Ticks */ 0, value = EmptyTuple) // Unit
    ))
}
