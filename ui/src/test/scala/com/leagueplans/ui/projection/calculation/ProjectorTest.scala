package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats
import com.leagueplans.ui.model.player.{GridStatus, Player}
import org.scalajs.dom.AbortController
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.{ExecutionContext, Future}

final class ProjectorTest extends AsyncFreeSpec with Matchers {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global
  // foldLeftAsyncHelper test infrastructure
  private type Iter = Projector.Iteration[Int, String]
  private val collect: (List[(String, Int)], String, Int) => List[(String, Int)] = (acc, v, r) => acc :+ (v, r)
  private def iter(id: Int, value: String, reps: Int): Iter = (id = id, value = value, reps = reps)
  private def iterForest(nodes: Map[Int, Iter], parentsToChildren: Map[Int, List[Int]] = Map.empty, roots: List[Int]): Forest[Int, Iter] =
    Forest.from(nodes, parentsToChildren, roots)

  // computeAsync test infrastructure
  private val noOpResolver: EffectResolver = (player, _) => player
  private val emptyPlayer = Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Set.empty), GridStatus(Set.empty))
  private val settings = Plan.Settings.Explicit(emptyPlayer, List.empty, None)
  private val projector = new Projector(noOpResolver)

  // Helper for foldLeftAsyncHelper tests. Uses a large yield interval by default so
  // reps are never split mid-batch, keeping traversal-correctness tests readable.
  private def runAsync(
    f: Forest[Int, Iter],
    remaining: List[Iter] = null,
    yieldInterval: Int = 1000
  ): Future[Option[List[(String, Int)]]] = {
    val rem = if (remaining == null) f.roots.flatMap(f.get) else remaining
    Projector.foldLeftAsyncHelper(f, List.empty, rem, new AbortController().signal, yieldInterval)(collect)
  }
  
  "foldLeftAsyncHelper" - {
    "returns None if signal is already aborted" in {
      val controller = new AbortController()
      controller.abort()
      Projector.foldLeftAsyncHelper(
        iterForest(Map(1 -> iter(1, "a", 3)), roots = List(1)),
        List.empty,
        List(iter(1, "a", 3)),
        controller.signal,
        yieldInterval = 1
      )(collect).map(_ shouldBe None)
    }

    "returns None if signal is aborted mid-computation" in {
      val f = iterForest(Map(1 -> iter(1, "a", 2)), roots = List(1))
      val controller = new AbortController()
      // yieldInterval=1 causes a yield after the first rep. Aborting synchronously
      // here (before the continuation fires) means the next iteration sees aborted=true.
      val future = Projector.foldLeftAsyncHelper(f, List.empty, f.roots.flatMap(f.get), controller.signal, 1)(collect)
      controller.abort()
      future.map(_ shouldBe None)
    }

    "empty forest returns the accumulator unchanged" in {
      runAsync(iterForest(Map.empty, roots = List.empty), remaining = List.empty).map {
        _ shouldBe Some(List.empty)
      }
    }

    "single leaf" - {
      "calls f once with the full rep count" in {
        val f = iterForest(Map(1 -> iter(1, "a", 3)), roots = List(1))
        runAsync(f).map(_ shouldBe Some(List(("a", 3))))
      }

      "with zero reps is skipped" in {
        val f = iterForest(Map(1 -> iter(1, "a", 0)), roots = List(1))
        runAsync(f).map(_ shouldBe Some(List.empty))
      }
    }

    "multiple roots are processed in order" in {
      val f = iterForest(Map(1 -> iter(1, "a", 1), 2 -> iter(2, "b", 2)), roots = List(1, 2))
      runAsync(f).map(_ shouldBe Some(List(("a", 1), ("b", 2))))
    }

    "parent with children" - {
      "unrolls parent and children once per repetition" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 1)),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(("a", 1), ("b", 1), ("a", 1), ("b", 1))))
      }

      "preserves leaf child reps within each cycle" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 3)),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(("a", 1), ("b", 3), ("a", 1), ("b", 3))))
      }

      "children with zero reps are skipped within each cycle" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 0)),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(("a", 1), ("a", 1))))
      }

      "multiple children are processed in order" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 1), 2 -> iter(2, "b", 2), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2, 3)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(("a", 1), ("b", 2), ("c", 1))))
      }

      "multiple children are repeated with the parent across cycles" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 1), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2, 3)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(("a", 1), ("b", 1), ("c", 1), ("a", 1), ("b", 1), ("c", 1))))
      }
    }

    "nested parents" - {
      "are traversed depth-first" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 1), 2 -> iter(2, "b", 1), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2), 2 -> List(3)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(("a", 1), ("b", 1), ("c", 1))))
      }

      "repetitions at each level unroll independently" in {
        val f = iterForest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 2), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2), 2 -> List(3)),
          roots = List(1)
        )
        runAsync(f).map(_ shouldBe Some(List(
          ("a", 1), ("b", 1), ("c", 1), ("b", 1), ("c", 1),
          ("a", 1), ("b", 1), ("c", 1), ("b", 1), ("c", 1)
        )))
      }
    }

    "yield batching splits leaf reps across yield boundaries" in {
      // With yieldInterval=2, a leaf with reps=5 is processed as batches of 2,2,1
      val f = iterForest(Map(1 -> iter(1, "a", 5)), roots = List(1))
      runAsync(f, yieldInterval = 2).map(_ shouldBe Some(List(("a", 2), ("a", 2), ("a", 1))))
    }
  }

  "computeAsync" - {
    "returns None if signal is already aborted" in {
      val controller = new AbortController()
      controller.abort()
      projector.computeAsync(Forest.empty, focusID = None, settings, controller.signal).map {
        _ shouldBe None
      }
    }

  }
}
