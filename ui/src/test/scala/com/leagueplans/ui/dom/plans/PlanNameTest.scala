package com.leagueplans.ui.dom.plans

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class PlanNameTest extends AnyFreeSpec with Matchers {
  "PlanName" - {
    "cleaning a typed name" - {
      "drops surrounding whitespace" in {
        PlanName.clean("  Leagues route \t") shouldEqual Some("Leagues route")
      }

      "keeps whitespace inside the name" in {
        PlanName.clean("Leagues  route") shouldEqual Some("Leagues  route")
      }

      "rejects names with nothing in them" in {
        PlanName.clean("") shouldEqual None
        PlanName.clean("  \t ") shouldEqual None
      }
    }

    "checking whether typing a name renames the plan" - {
      "is true for a different name" in {
        PlanName.isChange("Leagues route 2", "Leagues route") shouldBe true
      }

      "is false for the same name, even with spaces around it" in {
        PlanName.isChange("Leagues route", "Leagues route") shouldBe false
        PlanName.isChange(" Leagues route ", "Leagues route") shouldBe false
      }

      "is false for an empty name" in {
        PlanName.isChange("  ", "Leagues route") shouldBe false
      }

      "counts a change of case" in {
        PlanName.isChange("leagues route", "Leagues route") shouldBe true
      }
    }

    "matching a typed name against a plan's name, to confirm a deletion" - {
      "ignores case" in {
        PlanName.matches("leagues ROUTE", "Leagues route") shouldBe true
      }

      "ignores surrounding whitespace on both sides" in {
        PlanName.matches("  Leagues route\n", "Leagues route") shouldBe true
        PlanName.matches("Leagues route", " Leagues route ") shouldBe true
      }

      "rejects a name that is only part of the plan's" in {
        PlanName.matches("Leagues", "Leagues route") shouldBe false
        PlanName.matches("", "Leagues route") shouldBe false
      }

      "rejects a name with different spacing inside" in {
        PlanName.matches("Leagues  route", "Leagues route") shouldBe false
      }
    }
  }
}
