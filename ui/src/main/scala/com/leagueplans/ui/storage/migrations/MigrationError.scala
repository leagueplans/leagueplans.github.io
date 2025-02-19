package com.leagueplans.ui.storage.migrations

import com.leagueplans.ui.storage.model.SchemaVersion

enum MigrationError(val message: String) {
  case UnsupportedInputSchema(
    input: SchemaVersion,
    supported: SchemaVersion
  ) extends MigrationError(s"Unsupported schema version [$input] supplied to migrator [$supported]")

  case Custom(override val message: String) extends MigrationError(message)
}
