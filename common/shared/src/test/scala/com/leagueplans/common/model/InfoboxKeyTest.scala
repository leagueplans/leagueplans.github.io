package com.leagueplans.common.model

import com.leagueplans.common.JsonSpec
import io.circe.Json
import org.scalatest.Assertion

final class InfoboxKeyTest extends JsonSpec {
  "InfoboxKey" - {
    "encoding values to and decoding values from an expected encoding" - {
      def expected(pageID: Int, version: String*): Json =
        Json.arr(Json.fromInt(pageID), Json.arr(version.map(Json.fromString)*))

      def test(key: InfoboxKey, expectedJson: Json): Assertion =
        testRoundTripSerialisation(key, expectedJson)

      "without a version" in test(InfoboxKey(259573, List.empty), expected(259573))
      "with one version" in test(InfoboxKey(9876, List("Normal")), expected(9876, "Normal"))
      "with several versions" in
        test(InfoboxKey(9876, List("Normal", "Broken")), expected(9876, "Normal", "Broken"))
    }
  }
}
