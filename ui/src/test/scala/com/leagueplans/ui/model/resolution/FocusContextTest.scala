package com.leagueplans.ui.model.resolution

import com.leagueplans.common.model.Skill
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.*
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.{Exp, Stats}
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import com.raquo.airstream.core.Signal
import com.raquo.airstream.ownership.ManualOwner
import com.raquo.airstream.state.Var
import org.scalatest.TryValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class FocusContextTest extends AnyFreeSpec with Matchers with TryValues {
  private val emptyPlayer = Player(
    stats = Stats(),
    depositories = Map.empty,
    completedQuests = Set.empty,
    completedDiaryTasks = Set.empty,
    leagueStatus = LeagueStatus(0, Set.empty, Set.empty),
    gridStatus = GridStatus(Set.empty)
  )

  private val resolver = new EffectResolver(
    expMultipliers = List.empty,
    leaguePointScoring = _ => 0,
    cache = Cache(Set.empty, Set.empty, Set.empty, Set.empty, Set.empty)
  )

  /** Creates a Step that gains the given amount of Woodcutting xp per effect application. */
  private def makeStep(id: String, xp: Int, reps: Int = 1): Step =
    Step(
      Step.ID.fromString(id),
      StepDetails(
        "test",
        EffectList(List(Effect.GainExp(Skill.Woodcutting, Exp(xp)))),
        List.empty,
        reps,
        Duration.ticks(0)
      )
    )

  /** Returns the raw Woodcutting xp stored in the player's stats.
    * Exp(n) stores n * 10 internally, so GainExp(Wc, Exp(n)) applied once adds n * 10 raw. */
  private def wc(player: Player): Int =
    player.stats(Skill.Woodcutting).raw / 10

  private def makeForest(
    nodes: Map[Step.ID, Step],
    parentsToChildren: Map[Step.ID, List[Step.ID]] = Map.empty,
    roots: List[Step.ID]
  ): Forest[Step.ID, Step] =
    Forest.from(nodes, parentsToChildren, roots)

  private def now[A](signal: Signal[A])(using owner: ManualOwner): A =
    signal.observe(using owner).now()

  private def withContext(
    focusID: Option[Step.ID] = None,
    forest: Forest[Step.ID, Step] = Forest.empty,
    initialPlayer: Player = emptyPlayer
  )(f: (FocusContext, Var[Option[Step.ID]], Var[Forest[Step.ID, Step]]) => ManualOwner ?=> ?): Unit = {
    val focusIDVar = Var(focusID)
    val forestVar = Var(forest)
    val ctx = FocusContext(initialPlayer, focusIDVar.signal, forestVar.signal, resolver)
    Using(new ManualOwner)(
      f(ctx, focusIDVar, forestVar)(using _)
    )(using _.killSubscriptions()).success: Unit
  }

  "step" - {
    "is None when focusID is None" in withContext() { (ctx, _, _) =>
      now(ctx.focus).shouldBe(None)
    }

    "is Some when focusID points to an existing step" in {
      val s = makeStep("a", 10)
      val f = makeForest(Map(s.id -> s), roots = List(s.id))
      withContext(focusID = Some(s.id), forest = f) { (ctx, _, _) =>
        now(ctx.focus).shouldBe(Some(s))
      }
    }

    "is None when focusID references a nonexistent step ID" in {
      withContext(focusID = Some(Step.ID.fromString("missing"))) { (ctx, _, _) =>
        now(ctx.focus).shouldBe(None)
      }
    }

    "updates when focusID changes" in {
      val a = makeStep("a", 10)
      val b = makeStep("b", 20)
      val f = makeForest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id))
      withContext(focusID = Some(a.id), forest = f) { (ctx, focusIDVar, _) =>
        now(ctx.focus).shouldBe(Some(a))
        focusIDVar.set(Some(b.id))
        now(ctx.focus).shouldBe(Some(b))
      }
    }

    "updates when the forest changes" in {
      val a = makeStep("a", 10)
      withContext(focusID = Some(a.id)) { (ctx, _, forestVar) =>
        now(ctx.focus).shouldBe(None)
        forestVar.set(makeForest(Map(a.id -> a), roots = List(a.id)))
        now(ctx.focus).shouldBe(Some(a))
      }
    }
  }

  "playerAfterFirstRepOfCurrentFocus" - {
    "when focusID is None, applies all steps to initialPlayer" in {
      val a = makeStep("a", 10)
      val b = makeStep("b", 20)
      val f = makeForest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id))
      withContext(forest = f) { (ctx, _, _) =>
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(30)
      }
    }

    "applies the focused step's directEffects exactly once, ignoring repetitions" in {
      val s = makeStep("s", 10, reps = 5)
      val f = makeForest(Map(s.id -> s), roots = List(s.id))
      withContext(focusID = Some(s.id), forest = f) { (ctx, _, _) =>
        // directEffects applied once (not 5 times)
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(10)
      }
    }

    "does not include effects from child steps" in {
      val parent = makeStep("parent", 10)
      val child = makeStep("child", 99)
      val f = makeForest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      )
      withContext(focusID = Some(parent.id), forest = f) { (ctx, _, _) =>
        // Only parent's 10 xp, not child's 99 xp
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(10)
      }
    }

    "updates when focusID changes" in {
      val a = makeStep("a", 10)
      val b = makeStep("b", 20)
      val f = makeForest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id))
      withContext(focusID = Some(a.id), forest = f) { (ctx, focusIDVar, _) =>
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(10)
        focusIDVar.set(Some(b.id))
        // playerBefore includes a(10) = 10; then b adds 20 → 30
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(30)
      }
    }

    "caps ancestor steps at 1 repetition" in {
      val root = makeStep("root", 10, reps = 5)
      val focused = makeStep("focused", 2)
      val f = makeForest(
        Map(root.id -> root, focused.id -> focused),
        parentsToChildren = Map(root.id -> List(focused.id)),
        roots = List(root.id)
      )
      withContext(focusID = Some(focused.id), forest = f) { (ctx, _, _) =>
        // root is an ancestor → capped to min(5, 1) = 1 rep = 10 xp
        // playerAfterFirst = 10 + 2 (focused) = 12
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(12)
      }
    }

    "preserves 0 repetitions for ancestors (does not force to 1)" in {
      val root = makeStep("root", 10, reps = 0)
      val focused = makeStep("focused", 2)
      val f = makeForest(
        Map(root.id -> root, focused.id -> focused),
        parentsToChildren = Map(root.id -> List(focused.id)),
        roots = List(root.id)
      )
      withContext(focusID = Some(focused.id), forest = f) { (ctx, _, _) =>
        // root has 0 reps → min(0, 1) = 0, so no root effects applied
        // playerAfterFirst = 0 + 2 (focused) = 2
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(2)
      }
    }

    "applies non-ancestor sibling steps using their full repetitions" in {
      val root = makeStep("root", 0, reps = 1)
      val sibling = makeStep("sibling", 10, reps = 4)
      val focused = makeStep("focused", 2)
      val f = makeForest(
        Map(root.id -> root, sibling.id -> sibling, focused.id -> focused),
        parentsToChildren = Map(root.id -> List(sibling.id, focused.id)),
        roots = List(root.id)
      )
      withContext(focusID = Some(focused.id), forest = f) { (ctx, _, _) =>
        // root: ancestor, capped at 1 rep, contributes 0 xp
        // sibling: not an ancestor, full 4 reps = 10*4 = 40 xp
        // playerAfterFirst = 40 + 2 (focused) = 42
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(42)
      }
    }

    "includes effects from all steps in trees preceding the focused step's containing tree" in {
      val outsideRoot = makeStep("outsideRoot", 5, reps = 2)
      val outsideChild = makeStep("outsideChild", 3)
      val root = makeStep("root", 7)
      val focused = makeStep("focused", 2)
      val f = makeForest(
        Map(
          outsideRoot.id -> outsideRoot,
          outsideChild.id -> outsideChild,
          root.id -> root,
          focused.id -> focused
        ),
        parentsToChildren = Map(
          outsideRoot.id -> List(outsideChild.id),
          root.id -> List(focused.id)
        ),
        roots = List(outsideRoot.id, root.id)
      )
      withContext(focusID = Some(focused.id), forest = f) { (ctx, _, _) =>
        // outsideRoot subtree (preceding tree, full reps):
        //   StepSeries: outsideRoot(1)+outsideChild(1)+outsideRoot(1)+outsideChild(1) = 5+3+5+3 = 16 xp
        // root: ancestor of focused, capped to 1 rep → 7 xp
        // playerAfterFirst = 16 + 7 + 2 (focused) = 25
        wc(now(ctx.playerAfterFirstRepOfCurrentFocus)).shouldBe(25)
      }
    }
  }

  "playerAfterAllRepsOfCurrentFocus" - {
    "when focusID is None, applies all steps to initialPlayer" in {
      val a = makeStep("a", 10)
      val b = makeStep("b", 20)
      val f = makeForest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id))
      withContext(forest = f) { (ctx, _, _) =>
        wc(now(ctx.playerAfterAllRepsOfCurrentFocus)).shouldBe(30)
      }
    }

    "for a focused leaf step with 1 repetition equals playerAfterFirstRep" in {
      val s = makeStep("s", 10, reps = 1)
      val f = makeForest(Map(s.id -> s), roots = List(s.id))
      withContext(focusID = Some(s.id), forest = f) { (ctx, _, _) =>
        now(ctx.playerAfterAllRepsOfCurrentFocus).shouldBe(now(ctx.playerAfterFirstRepOfCurrentFocus))
      }
    }

    "applies focused step's directEffects for all repetitions" in {
      val s = makeStep("s", 10, reps = 3)
      val f = makeForest(Map(s.id -> s), roots = List(s.id))
      withContext(focusID = Some(s.id), forest = f) { (ctx, _, _) =>
        // 10 xp × 3 reps = Exp(30)
        wc(now(ctx.playerAfterAllRepsOfCurrentFocus)).shouldBe(30)
      }
    }

    "includes effects from child steps" in {
      val parent = makeStep("parent", 10)
      val child = makeStep("child", 5)
      val f = makeForest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      )
      withContext(focusID = Some(parent.id), forest = f) { (ctx, _, _) =>
        // parent(1) + child(1) = 10 + 5 = 15
        wc(now(ctx.playerAfterAllRepsOfCurrentFocus)).shouldBe(15)
      }
    }

    "repeats the entire subtree once per parent repetition" in {
      val parent = makeStep("parent", 10, reps = 2)
      val child = makeStep("child", 5, reps = 3)
      val f = makeForest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      )
      withContext(focusID = Some(parent.id), forest = f) { (ctx, _, _) =>
        // StepSeries: parent(1)+child(3)+parent(1)+child(3) = 10+15+10+15 = 50
        wc(now(ctx.playerAfterAllRepsOfCurrentFocus)).shouldBe(50)
      }
    }

    "includes effects from all steps in trees preceding the focused step's containing tree" in {
      val preRoot = makeStep("preRoot", 5, reps = 2)
      val preChild = makeStep("preChild", 3)
      val focused = makeStep("focused", 4, reps = 3)
      val f = makeForest(
        Map(preRoot.id -> preRoot, preChild.id -> preChild, focused.id -> focused),
        parentsToChildren = Map(preRoot.id -> List(preChild.id)),
        roots = List(preRoot.id, focused.id)
      )
      withContext(focusID = Some(focused.id), forest = f) { (ctx, _, _) =>
        // preRoot subtree (preceding tree, full reps):
        //   StepSeries: preRoot(1)+preChild(1)+preRoot(1)+preChild(1) = 5+3+5+3 = 16 xp
        // focused: all 3 reps applied = 4 × 3 = 12 xp
        // total = 16 + 12 = 28
        wc(now(ctx.playerAfterAllRepsOfCurrentFocus)).shouldBe(28)
      }
    }

    "caps ancestor steps at 1 repetition while applying the focused step's full repetitions" in {
      val root = makeStep("root", 10, reps = 3)
      val focused = makeStep("focused", 5, reps = 4)
      val f = makeForest(
        Map(root.id -> root, focused.id -> focused),
        parentsToChildren = Map(root.id -> List(focused.id)),
        roots = List(root.id)
      )
      withContext(focusID = Some(focused.id), forest = f) { (ctx, _, _) =>
        // root: ancestor → capped to min(3, 1) = 1 rep = 10 xp (not 30)
        // focused: all 4 reps applied = 5 × 4 = 20 xp
        // total = 10 + 20 = 30
        wc(now(ctx.playerAfterAllRepsOfCurrentFocus)).shouldBe(30)
      }
    }
  }
}
