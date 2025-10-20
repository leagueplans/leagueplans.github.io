package com.leagueplans.ui.storage.migrations

final class V2PlanMigrationTest extends PlanMigrationSpec(
  V2PlanMigration,
  testCases = "empty", "complex"
)
