package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

/** A step's repetitions can be a condition to repeat until, where V10's were only ever a
  * number. Each number becomes a fixed number of repetitions.
  */
object V11PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V10
  val toVersion: SchemaVersion = SchemaVersion.V11

  // The case's position in the repetitions enum, which is how the encoding names it
  private val fixed = 0

  def apply(plan: PlanExport): MigrationResult[PlanExport] =
    for {
      (name, timestamp, schemaVersion) <- plan.metadata.as[(Encoding, Encoding, SchemaVersion)]
      _ <- validateInputVersion(schemaVersion)
      updatedSteps <- migrateList(plan.steps.toList)((id, details) =>
        migrateDetails(details).map((id, _))
      )
    } yield plan.copy(
      metadata = Encoder.encode((name, timestamp, toVersion)),
      steps = updatedSteps.toMap
    )

  private def migrateDetails(details: Encoding): MigrationResult[Encoding] =
    details.as[(Encoding, List[Encoding], List[Encoding], Encoding, Encoding)].map(
      (description, effects, requirements, repetitions, duration) =>
        Encoder.encode((description, effects, requirements, encodeCoproduct(fixed, Tuple1(repetitions)), duration))
    )
}
