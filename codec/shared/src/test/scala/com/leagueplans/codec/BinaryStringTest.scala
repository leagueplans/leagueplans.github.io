package com.leagueplans.codec

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class BinaryStringTest extends AnyFreeSpec with Matchers {
  "BinaryString" - {
    "should convert integers to their unsigned binary form" - {
      "0" in(BinaryString(0) shouldEqual "0")

      "1" in(BinaryString(1) shouldEqual "1")
      "2" in(BinaryString(2) shouldEqual "10")
      "Int.MaxValue" in(BinaryString(Int.MaxValue) shouldEqual "1".repeat(31))

      "-1" in(BinaryString(-1) shouldEqual "1".repeat(32))
      "-2" in(BinaryString(-2) shouldEqual s"${"1".repeat(31)}0")
      "Int.MinValue" in(BinaryString(Int.MinValue) shouldEqual s"1${"0".repeat(31)}")
    }

    "should convert longs to their unsigned binary form" - {
      "0" in(BinaryString(0L) shouldEqual "0")

      "1" in(BinaryString(1L) shouldEqual "1")
      "2" in(BinaryString(2L) shouldEqual "10")
      "Long.MaxValue" in(BinaryString(Long.MaxValue) shouldEqual "1".repeat(63))

      "-1" in(BinaryString(-1L) shouldEqual "1".repeat(64))
      "-2" in(BinaryString(-2L) shouldEqual s"${"1".repeat(63)}0")
      "Long.MinValue" in(BinaryString(Long.MinValue) shouldEqual s"1${"0".repeat(63)}")
    }

    "unsafe" - {
      "should trim leading zeros from the input" in(
        BinaryString.unsafe("000001110") shouldEqual "1110"
      )

      "should return 0 for the empty string" in(
        BinaryString.unsafe("") shouldEqual "0"
      )

      "should return 0 when passed a string only contain 0s" in(
        BinaryString.unsafe("000000") shouldEqual "0"
      )
    }
  }
}
