package com.leagueplans.uicommon.utils.scala

import com.leagueplans.uicommon.utils.scala.IntOps.{ordinal, withCommas}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class IntOpsTest extends AnyFreeSpec with Matchers {
  "IntOps" - {
    "withCommas" - {
      "separates thousands" in {
        1234567.withCommas shouldBe "1,234,567"
      }
    }

    "ordinal" - {
      "ends 1, 2 and 3 in st, nd and rd" in {
        List(1, 2, 3, 21, 22, 23, 101, 102, 103).map(_.ordinal) shouldBe
          List("1st", "2nd", "3rd", "21st", "22nd", "23rd", "101st", "102nd", "103rd")
      }

      "ends everything else in th" in {
        List(0, 4, 9, 10, 20, 100).map(_.ordinal) shouldBe List("0th", "4th", "9th", "10th", "20th", "100th")
      }

      "ends the teens in th" in {
        List(11, 12, 13, 111, 112, 113, 1011).map(_.ordinal) shouldBe
          List("11th", "12th", "13th", "111th", "112th", "113th", "1,011th")
      }

      "separates thousands" in {
        1002.ordinal shouldBe "1,002nd"
      }
    }
  }
}
