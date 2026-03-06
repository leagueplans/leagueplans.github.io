package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

object V6PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V5
  val toVersion: SchemaVersion = SchemaVersion.V6

  def apply(plan: PlanExport): MigrationResult[PlanExport] =
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
      .as[(Encoding, List[Encoding], List[Encoding], Int, Encoding)]
      .map((description, effects, requirements, repetitions, duration) =>
        Encoder.encode(
          (description, effects, requirements, if repetitions == 0 then 1 else repetitions, duration)
        )
      )
}
