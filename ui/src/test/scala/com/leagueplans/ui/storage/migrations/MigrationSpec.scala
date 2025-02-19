package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.EncodingEqualities
import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.ui.storage.model.PlanExport
import org.scalatest.{EitherValues, TryValues}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.Dynamic
import scala.scalajs.js.typedarray.{ArrayBuffer, Int8Array, TA2AB}
import scala.util.{Try, Using}
import scala.util.Using.Releasable

abstract class MigrationSpec(migration: Migration, testCases: String*)
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

  // Based on https://stackoverflow.com/a/43396009
  private val fs = Dynamic.global.require("fs")

  private def readFile(fileName: String): Try[Array[Byte]] =
    // https://nodejs.org/api/fs.html#fsopensyncpath-flags-mode
    Using(fs.openSync(fileName))(descriptor =>
      // readFileSync does not close the underlying file handle, so we need to wrap it
      val buffer = fs.readFileSync(descriptor) // https://nodejs.org/api/fs.html#fsreadfilesyncpath-options
      new Int8Array( // https://nodejs.org/api/buffer.html#bufbyteoffset
        buffer.buffer.asInstanceOf[ArrayBuffer],
        buffer.byteOffset.asInstanceOf[Int],
        buffer.length.asInstanceOf[Int]
      ).toArray
    )(descriptor =>
      // https://nodejs.org/api/fs.html#fsclosesyncfd
      fs.closeSync(descriptor): @nowarn("msg=discarded non-Unit value")
    )

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
