package com.leagueplans.ui.model.player

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.projection.model.Projection
import com.raquo.airstream.ownership.ManualOwner
import com.raquo.airstream.state.Var
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class FocusContextTest extends AnyFreeSpec with Matchers {
  private val player = Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))
  private val (first, second) = (Step.ID.fromString("first"), Step.ID.fromString("second"))

  private def projectionFor(focus: Option[Step.ID]): Projection =
    Projection(player, player, player, focus)

  "FocusContext" - {
    "only gives the player before the focus once the projection was worked out for that step" in {
      val focus = Var(Option(first))
      val projection = Var(projectionFor(Some(first)))
      val context = FocusContext(focus.signal, Var(Forest.empty[Step.ID, Step]).signal, projection.signal)

      Using(new ManualOwner)(owner =>
        val playerBefore = context.playerBeforeFocusIfCurrent.observe(using owner)
        playerBefore.now() shouldBe Some(player)

        // The worker hasn't caught up with the new focus yet
        focus.set(Some(second))
        playerBefore.now() shouldBe None

        projection.set(projectionFor(Some(second)))
        playerBefore.now() shouldBe Some(player)

        focus.set(None)
        projection.set(projectionFor(None))
        playerBefore.now() shouldBe None
      )(using _.killSubscriptions()).get
    }
  }
}
