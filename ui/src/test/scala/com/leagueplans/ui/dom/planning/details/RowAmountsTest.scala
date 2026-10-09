package com.leagueplans.ui.dom.planning.details

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, Skill}
import com.leagueplans.ui.dom.planning.details.RowAmounts.{Amount, Tone}
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity, Requirement}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class RowAmountsTest extends AnyFreeSpec with Matchers {
  private val logs = Item.ID(1511)
  private val items: Item.ID => Item = Map(
    logs -> Item(
      logs,
      gameID = None,
      "Logs",
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1511/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = false,
      noteable = true,
      equipmentType = None,
      infobox = InfoboxKey(1, List.empty)
    )
  )
  private val gainExp: Effect.GainExp = Effect.GainExp(Skill.Woodcutting, Exp.tenths(12505))
  private val addLogs: Effect.AddItem = Effect.AddItem(logs, ItemChange.By(25), Depository.Kind.Inventory, note = false)
  private val removeLogs: Effect.AddItem = addLogs.copy(change = ItemChange.By(-25))
  private val moveLogs: Effect.MoveItem =
    Effect.MoveItem(logs, ItemQuantity.Exact(1250), Depository.Kind.Inventory, false, Depository.Kind.Bank, false)
  private val withdrawLogs: Effect.MoveItem =
    Effect.MoveItem(logs, ItemQuantity.Exact(5), Depository.Kind.Bank, false, Depository.Kind.Inventory, false)

  private def amountOf(effect: Effect): Option[Amount] =
    RowAmounts.of(effect, items)

  private def withAmount(effect: Effect, text: String): Either[String, Effect] =
    RowAmounts.withAmount(items)(effect, text)

  "RowAmounts.of" - {
    "shows exp with its tenth only when there is one" in {
      amountOf(gainExp) shouldBe Some(Amount("+1,250.5", "1250.5", Tone.Gain))
      amountOf(Effect.GainExp(Skill.Woodcutting, Exp(25))) shouldBe Some(Amount("+25", "25", Tone.Gain))
    }

    "shows whether items are added or removed" in {
      amountOf(addLogs) shouldBe Some(Amount("+25", "25", Tone.Gain, counted = true))
      amountOf(removeLogs) shouldBe Some(Amount("−25", "25", Tone.Loss, counted = true))
      amountOf(moveLogs) shouldBe Some(Amount("1,250", "1250", Tone.Neutral, counted = true))
    }

    "shows Max as max when it fills the inventory, and as all otherwise" in {
      amountOf(moveLogs.copy(quantity = ItemQuantity.Max)) shouldBe Some(Amount("all", "all", Tone.Neutral, counted = true))
      amountOf(withdrawLogs.copy(quantity = ItemQuantity.Max)) shouldBe Some(Amount("max", "max", Tone.Neutral, counted = true))
      amountOf(withdrawLogs.copy(quantity = ItemQuantity.Max, noteInTarget = true)) shouldBe
        Some(Amount("all", "all", Tone.Neutral, counted = true))
      amountOf(removeLogs.copy(change = ItemChange.Empty)) shouldBe Some(Amount("−all", "all", Tone.Loss, counted = true))
      amountOf(addLogs.copy(change = ItemChange.Fill)) shouldBe Some(Amount("+max", "max", Tone.Gain, counted = true))
    }

    "has no amount for completions" in {
      amountOf(Effect.CompleteQuest(3)) shouldBe None
      amountOf(Effect.UnlockSkill(Skill.Sailing)) shouldBe None
    }

    "shows required levels" in {
      RowAmounts.of(Requirement.SkillLevel(Skill.Agility, Level(50))) shouldBe Some(Amount("level 50", "50", Tone.Neutral))
      RowAmounts.of(Requirement.Holds(logs, Requirement.Where.Inventory)) shouldBe None
    }
  }

  "RowAmounts.withAmount" - {
    "sets exp exactly, including tenths" in {
      withAmount(gainExp, "37.5") shouldBe Right(gainExp.copy(baseExp = Exp.tenths(375)))
      withAmount(gainExp, "1,000") shouldBe Right(gainExp.copy(baseExp = Exp(1000)))
      withAmount(gainExp, "0.3") shouldBe Right(gainExp.copy(baseExp = Exp.tenths(3)))
    }

    "rejects exp that can't be stored" in {
      withAmount(gainExp, "0").isLeft shouldBe true
      withAmount(gainExp, "1.25").isLeft shouldBe true
      withAmount(gainExp, "200000001").isLeft shouldBe true
      withAmount(gainExp, "lots").isLeft shouldBe true
    }

    "sets item amounts" in {
      withAmount(removeLogs, "10") shouldBe Right(removeLogs.copy(change = ItemChange.By(-10)))
      withAmount(addLogs, "10") shouldBe Right(addLogs.copy(change = ItemChange.By(10)))
      withAmount(moveLogs, "10k") shouldBe Right(moveLogs.copy(quantity = ItemQuantity.Exact(10000)))
      withAmount(moveLogs, "2.5") shouldBe Right(moveLogs.copy(quantity = ItemQuantity.Exact(2)))
    }

    "sets Max from max, or all" in {
      withAmount(moveLogs, "Max") shouldBe Right(moveLogs.copy(quantity = ItemQuantity.Max))
      withAmount(removeLogs, "max") shouldBe Right(removeLogs.copy(change = ItemChange.Empty))
      withAmount(addLogs, "max") shouldBe Right(addLogs.copy(change = ItemChange.Fill))
      withAmount(addLogs, "all") shouldBe Right(addLogs.copy(change = ItemChange.Fill))
    }

    "rejects item amounts below 1" in {
      withAmount(moveLogs, "0").isLeft shouldBe true
      withAmount(moveLogs, "-5").isLeft shouldBe true
      withAmount(moveLogs, "0.5").isLeft shouldBe true
    }

    "suggests the word the row shows for the most" in {
      withAmount(moveLogs, "lots") shouldBe Left("Type an amount from 1, such as 250 or 1.5k, or all")
      withAmount(withdrawLogs, "lots") shouldBe Left("Type an amount from 1, such as 250 or 1.5k, or max")
    }

    "sets levels from 1 to 99" in {
      val requirement: Requirement.SkillLevel = Requirement.SkillLevel(Skill.Agility, Level(50))
      RowAmounts.withAmount(requirement, "60") shouldBe Right(requirement.copy(level = Level(60)))
      RowAmounts.withAmount(requirement, "100").isLeft shouldBe true
      RowAmounts.withAmount(requirement, "0").isLeft shouldBe true
    }
  }
}
