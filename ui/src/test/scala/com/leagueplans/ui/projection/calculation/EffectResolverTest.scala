package com.leagueplans.ui.projection.calculation

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.plan.{Effect, ExpMultiplier}
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Stats}
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class EffectResolverTest extends AnyFreeSpec with Matchers {
  private val cache = Cache(Set.empty, Set.empty, Set.empty, Set.empty, Set.empty)
  private val player = Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))

  private def resolver(multiplier: Double): EffectResolver =
    EffectResolver(
      List(ExpMultiplier(Skill.values.toSet, ExpMultiplier.Kind.Multiplicative, multiplier, List.empty)),
      _ => 0,
      cache
    )

  "EffectResolver" - {
    "gaining exp" - {
      "applies the multiplier" in {
        resolver(5).resolve(player, Effect.GainExp(Skill.Attack, Exp(10))).stats(Skill.Attack) shouldBe Exp(50)
      }

      "stops at 200M exp, as the game does" in {
        resolver(1).resolve(player, Effect.GainExp(Skill.Attack, Exp(150000000)), Effect.GainExp(Skill.Attack, Exp(150000000)))
          .stats(Skill.Attack) shouldBe Exp.max
      }

      "doesn't wrap round when the exp after the multiplier passes what an Int holds" in {
        resolver(16).resolve(player, Effect.GainExp(Skill.Attack, Exp(150000000)), Effect.GainExp(Skill.Attack, Exp(150000000)))
          .stats(Skill.Attack) shouldBe Exp.max
      }
    }
  }
}
