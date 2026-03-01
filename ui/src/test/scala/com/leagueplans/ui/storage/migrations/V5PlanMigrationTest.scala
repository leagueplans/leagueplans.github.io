package com.leagueplans.ui.storage.migrations

final class V5PlanMigrationTest extends PlanMigrationSpec(
  V5PlanMigration,
  testCases = 
    "single-step",
    "complex"
)
