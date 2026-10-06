package com.leagueplans.ui.storage.migrations

final class V9PlanMigrationTest extends PlanMigrationSpec(
  V9PlanMigration,
  testCases =
    "item-quantities",
    "item-requirements",
    "real-plan"
)
