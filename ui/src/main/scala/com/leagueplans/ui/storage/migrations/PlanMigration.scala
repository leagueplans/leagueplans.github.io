package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.storage.model.{PlanExport, SchemaVersion}

trait PlanMigration {
  def fromVersion: SchemaVersion
  def toVersion: SchemaVersion

  def apply(plan: PlanExport): Either[DecodingFailure | MigrationError, PlanExport]

  final protected def validateInputVersion(input: SchemaVersion): Either[MigrationError, Unit] =
    Either.cond(
      input == fromVersion,
      right = (),
      left = MigrationError.UnsupportedInputSchema(input = input, supported = fromVersion)
    )
}
