package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

/** Exp effects hold a number of actions and the exp each gives, where V9's held only their exp.
  * Each becomes a single action of all of its exp.
  */
object V10PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V9
  val toVersion: SchemaVersion = SchemaVersion.V10

  // The effect's position in the effect enum, which is how the encoding names it
  private val gainExp = 0

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
    for {
      (description, effects, requirements, repetitions, duration) <-
        details.as[(Encoding, List[Encoding], List[Encoding], Encoding, Encoding)]
      updatedEffects <- migrateList(effects)(migrateEffect)
    } yield Encoder.encode((description, updatedEffects, requirements, repetitions, duration))

  private def migrateEffect(effect: Encoding): MigrationResult[Encoding] =
    decodeOrdinal(effect).flatMap {
      case (`gainExp`, fields) =>
        fields.as[(Encoding, Encoding)].map((skill, baseExp) =>
          encodeCoproduct(gainExp, (skill, 1, baseExp))
        )
      case _ =>
        Right(effect)
    }
}
