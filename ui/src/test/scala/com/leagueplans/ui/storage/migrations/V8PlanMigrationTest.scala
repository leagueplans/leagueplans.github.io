package com.leagueplans.ui.storage.migrations

final class V8PlanMigrationTest extends PlanMigrationSpec(
  V8PlanMigration,
  testCases =
    "removed-quest",
    "unaffected"
)
