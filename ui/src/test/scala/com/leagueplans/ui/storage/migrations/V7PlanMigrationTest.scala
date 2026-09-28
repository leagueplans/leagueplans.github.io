package com.leagueplans.ui.storage.migrations

final class V7PlanMigrationTest extends PlanMigrationSpec(
  V7PlanMigration,
  testCases =
    "merged-items",
    "removed-items",
    "unaffected"
)
