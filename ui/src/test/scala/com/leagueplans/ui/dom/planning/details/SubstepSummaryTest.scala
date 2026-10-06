package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.dom.planning.details.SubstepSummary.Change
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Level, Stats}
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class SubstepSummaryTest extends AnyFreeSpec with Matchers {
  private val logs = Item.ID(1511)
  private val coins = Item.ID(995)
  private val start =
    Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))

  private def holding(kind: Depository.Kind, contents: ((Item.ID, Boolean), Int)*): Map[Depository.Kind, Depository] =
    Map(kind -> Depository(contents.toMap, kind))

  private val miniquest = 3

  private def between(before: Player, after: Player): List[Change] =
    SubstepSummary.between(before, after, Map(logs -> "Logs", coins -> "Coins"), isMiniquest = _ == miniquest)

  "SubstepSummary.between" - {
    "finds nothing when nothing changed" in {
      between(start, start) shouldBe List.empty
    }

    "reports exp gained, with the levels when they changed" in {
      val after = start.copy(stats = Stats(Skill.Woodcutting -> Exp(83), Skill.Agility -> Exp(10)))
      between(start, after) shouldBe List(
        Change.ExpGained(Skill.Agility, Exp(10), None),
        Change.ExpGained(Skill.Woodcutting, Exp(83), Some((Level(1), Level(2))))
      )
    }

    "reports items by name, whichever depository they're in" in {
      val before = start.copy(depositories =
        holding(Depository.Kind.Inventory, (logs, false) -> 5, (coins, false) -> 100)
      )
      val after = start.copy(depositories =
        holding(Depository.Kind.Bank, (logs, false) -> 30, (logs, true) -> 2, (coins, false) -> 100)
      )
      between(before, after) shouldBe List(
        Change.ItemsChanged(coins, noted = false, Depository.Kind.Inventory, -100),
        Change.ItemsChanged(coins, noted = false, Depository.Kind.Bank, 100),
        Change.ItemsChanged(logs, noted = false, Depository.Kind.Inventory, -5),
        Change.ItemsChanged(logs, noted = false, Depository.Kind.Bank, 30),
        Change.ItemsChanged(logs, noted = true, Depository.Kind.Bank, 2)
      )
    }

    "counts completions, with miniquests apart from quests, and league points" in {
      val after = start.copy(
        completedQuests = Set(1, 2, miniquest),
        leagueStatus = LeagueStatus(30, Set(7), Set(Skill.Sailing)),
        gridStatus = GridStatus(Set(3))
      )
      between(start, after) shouldBe List(
        Change.SkillsUnlocked(List(Skill.Sailing)),
        Change.Completed("Quests", 2),
        Change.Completed("Miniquests", 1),
        Change.Completed("League tasks", 1),
        Change.Completed("Grid tiles", 1),
        Change.LeaguePointsGained(30)
      )
    }
  }
}
