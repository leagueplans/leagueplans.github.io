package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.Encoding
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

object V4PlanMigration extends PlanMigration {
  val fromVersion: SchemaVersion = SchemaVersion.V3
  val toVersion: SchemaVersion = SchemaVersion.V4

  private val itemIDMigrations: Map[Int, Int] =
    Map(
      13218 -> 11670, // Sage's greaves
        707 -> 11671, // Arcane grimoire
      13200 -> 11671, // Arcane grimoire
      10540 -> 11714, // Trailblazer axe
      10556 -> 11715, // Trailblazer pickaxe
      10550 -> 11716, // Trailblazer harpoon
       4170 -> 13206, // Fairy mushroom
      11678 -> 13206, // Fairy mushroom
       3062 -> 13207, // Crystal of memories/echoes
      11673 -> 13207, // Crystal of memories/echoes
      11672 -> 13207, // Crystal of memories/echoes
      11674 -> 13208, // Banker's note
      11680 -> 13219, // Guardian horn
      14044 -> 14045, // Stone tablet (The Final Dawn)
    )

  def apply(plan: PlanExport): Either[DecodingFailure | MigrationError, PlanExport] =
    for {
      (name, timestamp, schemaVersion) <- plan.metadata.as[(Encoding, Encoding, SchemaVersion)]
      _ <- validateInputVersion(schemaVersion)
      updatedSteps <- ItemIDMigrator(itemIDMigrations, plan.steps)
    } yield PlanExport(
      Encoder.encode((name, timestamp, toVersion)),
      plan.settings,
      plan.mappings,
      updatedSteps
    )
}
