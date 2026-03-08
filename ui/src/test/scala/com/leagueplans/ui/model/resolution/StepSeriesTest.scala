package com.leagueplans.ui.model.resolution

import com.leagueplans.ui.model.common.forest.Forest
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StepSeriesTest extends AnyFreeSpec with Matchers {
  private type Iter = StepSeries.Iteration[Int, String]

  private val collect: (List[(String, Int)], String, Int) => List[(String, Int)] =
    (acc, v, r) => acc :+ (v, r)

  private def iter(id: Int, value: String, reps: Int): Iter =
    (id = id, value = value, reps = reps)

  private def forest(
    nodes: Map[Int, Iter],
    parentsToChildren: Map[Int, List[Int]] = Map.empty,
    roots: List[Int]
  ): Forest[Int, Iter] =
    Forest.from(nodes, parentsToChildren, roots)

  "foldLeftHelper" - {
    "empty forest returns the accumulator unchanged" in {
      StepSeries.foldLeftHelper(forest(Map.empty, roots = List.empty), List.empty)(collect) shouldBe List.empty
    }

    "single leaf" - {
      "calls f once with the full rep count" in {
        val f = forest(Map(1 -> iter(1, "a", 3)), roots = List(1))
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List(("a", 3))
      }

      "with zero reps is skipped" in {
        val f = forest(Map(1 -> iter(1, "a", 0)), roots = List(1))
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List.empty
      }
    }

    "multiple roots are processed in order" in {
      val f = forest(
        Map(1 -> iter(1, "a", 1), 2 -> iter(2, "b", 2)),
        roots = List(1, 2)
      )
      StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List(("a", 1), ("b", 2))
    }

    "parent with children" - {
      "unrolls parent and children once per repetition" in {
        val f = forest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 1)),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe
          List(("a", 1), ("b", 1), ("a", 1), ("b", 1))
      }

      "preserves leaf child reps within each cycle" in {
        val f = forest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 3)),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe
          List(("a", 1), ("b", 3), ("a", 1), ("b", 3))
      }

      "children with zero reps are skipped within each cycle" in {
        val f = forest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 0)),
          parentsToChildren = Map(1 -> List(2)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List(("a", 1), ("a", 1))
      }

      "multiple children are processed in order" in {
        val f = forest(
          Map(1 -> iter(1, "a", 1), 2 -> iter(2, "b", 2), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2, 3)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List(("a", 1), ("b", 2), ("c", 1))
      }

      "multiple children are repeated with the parent across cycles" in {
        val f = forest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 1), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2, 3)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe
          List(("a", 1), ("b", 1), ("c", 1), ("a", 1), ("b", 1), ("c", 1))
      }
    }

    "nested parents" - {
      "are traversed depth-first" in {
        val f = forest(
          Map(1 -> iter(1, "a", 1), 2 -> iter(2, "b", 1), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2), 2 -> List(3)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List(("a", 1), ("b", 1), ("c", 1))
      }

      "repetitions at each level unroll independently" in {
        val f = forest(
          Map(1 -> iter(1, "a", 2), 2 -> iter(2, "b", 2), 3 -> iter(3, "c", 1)),
          parentsToChildren = Map(1 -> List(2), 2 -> List(3)),
          roots = List(1)
        )
        StepSeries.foldLeftHelper(f, List.empty)(collect) shouldBe List(
          ("a", 1), ("b", 1), ("c", 1), ("b", 1), ("c", 1),
          ("a", 1), ("b", 1), ("c", 1), ("b", 1), ("c", 1)
        )
      }
    }
  }
}
