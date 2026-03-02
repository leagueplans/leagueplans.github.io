package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.EncodingEqualities
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.storage.model.PlanExport
import com.leagueplans.ui.testutils.readFile
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{EitherValues, TryValues}

abstract class PlanMigrationSpec(migration: PlanMigration, testCases: String*)
  extends AnyFreeSpec
    with Matchers
    with EitherValues
    with TryValues
    with EncodingEqualities {

  private def toTestPath(testCase: String): String =
    s"ui/src/test/resources/migration-samples/${migration.toVersion}/$testCase"

  private def readPlan(fileName: String): PlanExport =
    Decoder.decodeMessage[PlanExport](
      readFile(fileName).success.value
    ).value

  s"${migration.fromVersion} -> ${migration.toVersion}" - {
    testCases.foreach(testCase =>
      testCase in {
        val before = withClue("Reading before:")(readPlan(s"${toTestPath(testCase)}/before.plan"))
        val expected = withClue("Reading after:")(readPlan(s"${toTestPath(testCase)}/after.plan"))

        val afterMigrating = migration(before).value

        withClue("metadata:")(afterMigrating.metadata shouldEqual expected.metadata)
        withClue("mappings:")(afterMigrating.settings shouldEqual expected.settings)
        withClue("mappings:")(afterMigrating.mappings shouldEqual expected.mappings)
        withClue("steps:") {
          afterMigrating.steps.keySet should contain theSameElementsAs expected.steps.keySet
          afterMigrating.steps.keys.foreach(step =>
            withClue(step)(afterMigrating.steps(step) shouldEqual expected.steps(step))
          )
        }
      }
    )
  }
}
