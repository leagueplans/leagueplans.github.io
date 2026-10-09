package com.leagueplans.ui.model.plan

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.mode.{Annihilation, LeaguesVI, MainGame}
import com.leagueplans.ui.model.player.skill.{Level, Stats}
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import org.scalatest.LoneElement
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class MultiplierGroupsTest extends AnyFreeSpec with Matchers with LoneElement {
  private val cache = Cache(Set.empty, Set.empty, Set.empty, Set.empty, Set.empty)

  private def player(leaguePoints: Int = 0, combatSkills: Int = 1): Player =
    Player(
      Stats(Skill.combats.toList.map(_ -> Level(combatSkills).bound)*),
      Map.empty,
      Set.empty,
      Set.empty,
      LeagueStatus(leaguePoints, Set.empty, Skill.values.toSet),
      GridStatus(Set.empty)
    )

  private def format(d: Double): String =
    if (d == math.rint(d)) d.toLong.toString else d.toString

  "MultiplierGroups" - {
    "puts every skill in one group when they share their multipliers" in {
      val groups = MultiplierGroups(MainGame.settings.expMultipliers, player(), cache)
      groups.map(_.name) shouldBe List("All skills")
      groups.head.skills should have size Skill.values.length
      groups.head.multiplier shouldBe 1
      groups.head.next shouldBe None
    }

    "splits combat and other skills when their multipliers differ" in {
      MultiplierGroups(LeaguesVI.settings.expMultipliers, player(), cache).map(g => (g.name, g.multiplier)) shouldBe
        List("Combat skills" -> 5.0, "Other skills" -> 5.0)
    }

    "finds the next rise, how far off it is, and the multiplier it gives" in {
      val combat = MultiplierGroups(LeaguesVI.settings.expMultipliers, player(leaguePoints = 450), cache).head
      combat.next.map(rise => (rise.multiplier, rise.condition, rise.remaining)) shouldBe
        Some((8.0, "600 league points", 150))
      combat.next.map(_.progress).get shouldBe 0.75 +- 0.001
    }

    "takes the nearest rise when several multipliers rise with the same quantity" in {
      // Combat skills get an extra 1.5× at 1,200 points, which comes before the 12× at 5,200, so
      // they go from 8× to 12× there
      val combat = MultiplierGroups(LeaguesVI.settings.expMultipliers, player(leaguePoints = 700), cache).head
      combat.multiplier shouldBe 8
      combat.next.map(rise => (rise.multiplier, rise.remaining)) shouldBe Some((12.0, 500))
    }

    "rises with combat level in Deadman" in {
      val groups = MultiplierGroups(Annihilation.settings.expMultipliers, player(combatSkills = 40), cache)
      groups.map(_.name) shouldBe List("Combat skills", "Other skills")
      val combat = groups.head
      combat.multiplier shouldBe 10
      combat.next.map(rise => (rise.multiplier, rise.condition)) shouldBe Some((15.0, "combat level 61"))
      groups(1).next shouldBe None
    }

    "has no next rise once the highest multiplier is reached" in {
      val groups = MultiplierGroups(Annihilation.settings.expMultipliers, player(combatSkills = 99), cache)
      groups.head.multiplier shouldBe 20
      groups.head.next shouldBe None
    }

    "gives the bonuses, and the multiplier with none and with all of them" in {
      // As Grid Master sets them up, with tiles alone since rows need the grid's tiles
      def bonus(tile: Int) =
        ExpMultiplier(Skill.values.toSet, ExpMultiplier.Kind.Additive, base = 0, List(2.0 -> ExpMultiplier.Condition.GridTile(tile)))
      val multipliers = ExpMultiplier(Skill.values.toSet, ExpMultiplier.Kind.Additive, base = 4, List.empty) ::
        List(9, 16, 48).map(bonus)
      val group = MultiplierGroups(multipliers, player(), cache).loneElement
      group.bonuses.map(bonus => (bonus.earned, bonus.value, bonus.kind)) shouldBe
        List.fill(3)((false, 2.0, ExpMultiplier.Kind.Additive))
      (group.multiplier, group.withoutBonuses, group.withAllBonuses) shouldBe (5.0, 5.0, 11.0)
    }

    "describes a group's multiplier" in {
      val combat = MultiplierGroups(Annihilation.settings.expMultipliers, player(combatSkills = 40), cache).head
      MultiplierGroups.describe(combat, format) shouldBe "Combat skills: 10× xp, 15× at combat level 61"
    }
  }
}
