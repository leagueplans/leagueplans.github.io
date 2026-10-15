package com.leagueplans.ui.storage.migrations

import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation}
import com.leagueplans.ui.model.plan.{ExpTarget, Repetitions, StopCondition}
import org.scalatest.EitherValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class ItemIDMigratorTest extends AnyFreeSpec with Matchers with EitherValues {
  "ItemIDMigrator" - {
    "repetitions" - {
      val migrator = ItemIDMigrator(oldToNewIDs = Map(1 -> 2), removedIDs = Set(3))

      def migrate(repetitions: Repetitions): Repetitions =
        migrator.repetitions(Encoder.encode(repetitions)).value.as[Repetitions].value

      def count(id: Int): StopCondition =
        StopCondition.ItemCount(Item.ID(id), ItemLocation.Bank, ItemForms.Noted, Comparison.AtLeast, 1000, stopBefore = false)

      val skill = StopCondition.SkillReaches(Skill.Mining, ExpTarget.AtLevel(Level.L41), stopBefore = false)

      "leaves a fixed count alone" in {
        migrate(Repetitions.Fixed(7)) shouldEqual Repetitions.Fixed(7)
      }

      "moves an item count to the item it was folded into" in {
        migrate(Repetitions.Until(count(1), Some(50))) shouldEqual Repetitions.Until(count(2), Some(50))
      }

      "leaves other items alone" in {
        migrate(Repetitions.Until(count(4), None)) shouldEqual Repetitions.Until(count(4), None)
      }

      "moves items inside a tree" in {
        migrate(Repetitions.Until(StopCondition.Or(skill, StopCondition.And(count(1), count(4))), None)) shouldEqual
          Repetitions.Until(StopCondition.Or(skill, StopCondition.And(count(2), count(4))), None)
      }

      "keeps the other half of a tree with a removed item" in {
        migrate(Repetitions.Until(StopCondition.And(count(3), skill), None)) shouldEqual
          Repetitions.Until(skill, None)
      }

      "repeats once when every condition was on a removed item" in {
        migrate(Repetitions.Until(StopCondition.Or(count(3), count(3)), Some(9))) shouldEqual Repetitions.once
      }
    }
  }
}
