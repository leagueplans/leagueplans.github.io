package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

/** Removes references to quests that no longer exist. The step itself, and its
  * description, remain.
  */
object V8PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V7
  val toVersion: SchemaVersion = SchemaVersion.V8

  private val removedQuests: Set[Int] =
    Set(
      149 // Architectural Alliance
    )

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
      updatedEffects <- migrateList(effects)(effect =>
        isRemovedQuest(effect).map(Option.unless(_)(effect))
      ).map(_.flatten)
    } yield Encoder.encode((description, updatedEffects, requirements, repetitions, duration))

  private def isRemovedQuest(effect: Encoding): MigrationResult[Boolean] =
    decodeOrdinal(effect).flatMap {
      case (/* CompleteQuest */ 4, completeQuest) =>
        completeQuest.as[Tuple1[Int]].map { case Tuple1(quest) => removedQuests.contains(quest) }
      case _ =>
        Right(false)
    }
}
