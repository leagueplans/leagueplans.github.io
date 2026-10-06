package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.details.RowAmounts.{Amount, Tone}
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class RowAmountsTest extends AnyFreeSpec with Matchers {
  private val logs = Item.ID(1511)
  private val gainExp: Effect.GainExp = Effect.GainExp(Skill.Woodcutting, Exp.tenths(12505))
  private val addLogs: Effect.AddItem = Effect.AddItem(logs, ItemChange.By(25), Depository.Kind.Inventory, note = false)
  private val removeLogs: Effect.AddItem = addLogs.copy(change = ItemChange.By(-25))
  private val moveLogs: Effect.MoveItem =
    Effect.MoveItem(logs, ItemQuantity.Exact(1250), Depository.Kind.Inventory, false, Depository.Kind.Bank, false)

  "RowAmounts.of" - {
    "shows exp with its tenth only when there is one" in {
      RowAmounts.of(gainExp) shouldBe Some(Amount("+1,250.5", "1250.5", Tone.Gain))
      RowAmounts.of(Effect.GainExp(Skill.Woodcutting, Exp(25))) shouldBe Some(Amount("+25", "25", Tone.Gain))
    }

    "shows whether items are added or removed" in {
      RowAmounts.of(addLogs) shouldBe Some(Amount("+25", "25", Tone.Gain))
      RowAmounts.of(removeLogs) shouldBe Some(Amount("−25", "25", Tone.Loss))
      RowAmounts.of(moveLogs) shouldBe Some(Amount("1,250", "1250", Tone.Neutral))
    }

    "shows Max as max" in {
      RowAmounts.of(moveLogs.copy(quantity = ItemQuantity.Max)) shouldBe Some(Amount("max", "max", Tone.Neutral))
      RowAmounts.of(removeLogs.copy(change = ItemChange.Empty)) shouldBe Some(Amount("−max", "max", Tone.Loss))
      RowAmounts.of(addLogs.copy(change = ItemChange.Fill)) shouldBe Some(Amount("+max", "max", Tone.Gain))
    }

    "has no amount for completions" in {
      RowAmounts.of(Effect.CompleteQuest(3)) shouldBe None
      RowAmounts.of(Effect.UnlockSkill(Skill.Sailing)) shouldBe None
    }

    "shows required levels" in {
      RowAmounts.of(Requirement.SkillLevel(Skill.Agility, Level(50))) shouldBe Some(Amount("50", "50", Tone.Neutral))
      RowAmounts.of(Requirement.Holds(logs, Requirement.Where.Inventory)) shouldBe None
    }
  }

  "RowAmounts.withAmount" - {
    "sets exp exactly, including tenths" in {
      RowAmounts.withAmount(gainExp, "37.5") shouldBe Right(gainExp.copy(baseExp = Exp.tenths(375)))
      RowAmounts.withAmount(gainExp, "1,000") shouldBe Right(gainExp.copy(baseExp = Exp(1000)))
      RowAmounts.withAmount(gainExp, "0.3") shouldBe Right(gainExp.copy(baseExp = Exp.tenths(3)))
    }

    "rejects exp that can't be stored" in {
      RowAmounts.withAmount(gainExp, "0").isLeft shouldBe true
      RowAmounts.withAmount(gainExp, "1.25").isLeft shouldBe true
      RowAmounts.withAmount(gainExp, "200000001").isLeft shouldBe true
      RowAmounts.withAmount(gainExp, "lots").isLeft shouldBe true
    }

    "sets item amounts" in {
      RowAmounts.withAmount(removeLogs, "10") shouldBe Right(removeLogs.copy(change = ItemChange.By(-10)))
      RowAmounts.withAmount(addLogs, "10") shouldBe Right(addLogs.copy(change = ItemChange.By(10)))
      RowAmounts.withAmount(moveLogs, "10k") shouldBe Right(moveLogs.copy(quantity = ItemQuantity.Exact(10000)))
      RowAmounts.withAmount(moveLogs, "2.5") shouldBe Right(moveLogs.copy(quantity = ItemQuantity.Exact(2)))
    }

    "sets Max from max, or all" in {
      RowAmounts.withAmount(moveLogs, "Max") shouldBe Right(moveLogs.copy(quantity = ItemQuantity.Max))
      RowAmounts.withAmount(removeLogs, "max") shouldBe Right(removeLogs.copy(change = ItemChange.Empty))
      RowAmounts.withAmount(addLogs, "max") shouldBe Right(addLogs.copy(change = ItemChange.Fill))
      RowAmounts.withAmount(addLogs, "all") shouldBe Right(addLogs.copy(change = ItemChange.Fill))
    }

    "rejects item amounts below 1" in {
      RowAmounts.withAmount(moveLogs, "0").isLeft shouldBe true
      RowAmounts.withAmount(moveLogs, "-5").isLeft shouldBe true
      RowAmounts.withAmount(moveLogs, "0.5").isLeft shouldBe true
    }

    "sets levels from 1 to 99" in {
      val requirement: Requirement.SkillLevel = Requirement.SkillLevel(Skill.Agility, Level(50))
      RowAmounts.withAmount(requirement, "60") shouldBe Right(requirement.copy(level = Level(60)))
      RowAmounts.withAmount(requirement, "100").isLeft shouldBe true
      RowAmounts.withAmount(requirement, "0").isLeft shouldBe true
    }
  }
}
