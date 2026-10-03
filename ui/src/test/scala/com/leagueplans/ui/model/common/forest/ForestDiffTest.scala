package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.dom.planning.forest.ForestUpdateConsumer
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*
import com.leagueplans.ui.model.common.forest.ForestDiffTest.*
import com.leagueplans.ui.model.common.forest.ForestPropertyTest.*
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.projection.calculation.TimeKeeper
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

final class ForestDiffTest
  extends AnyFreeSpec
    with ForestAssertions
    with ScalaCheckDrivenPropertyChecks {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 200)

  /** The diff is valid for every consumer, and produces the target forest */
  private def checkDiff[ID, T](from: Forest[ID, T], to: Forest[ID, T], updates: List[Update[ID, T]]): Assertion =
    withClue(s"Diffing $from into $to, producing $updates:") {
      replayCarefully(from, updates) shouldEqual to
    }

  private def checkDiff[ID, T](from: Forest[ID, T], to: Forest[ID, T]): Assertion =
    checkDiff(from, to, ForestDiff.diff(from, to))

  /** Applies each update in turn, first checking that consumers can apply it */
  private def replayCarefully[ID, T](from: Forest[ID, T], updates: List[Update[ID, T]]): Forest[ID, T] =
    updates.foldLeft(from) { (forest, update) =>
      withClue(s"Before applying $update to $forest:") {
        update match {
          case AddNode(id, _) =>
            forest.contains(id) shouldBe false
          case RemoveNode(id) =>
            forest.roots should contain(id)
            forest.toChildren(id) shouldBe empty
          case AddLink(child, parent) =>
            forest.contains(parent) shouldBe true
            forest.roots should contain(child)
          case RemoveLink(child, parent) =>
            forest.toParent.get(child) shouldEqual Some(parent)
          case UpdateData(id, _) =>
            forest.contains(id) shouldBe true
          case Reorder(children, maybeParent) =>
            Slice.listState(forest, maybeParent).map(counts) shouldEqual Some(counts(children))
        }
      }
      val updated = ForestResolver.resolve(forest, update)
      withClue(s"After applying $update to $forest:") {
        Forest.acyclic(updated.nodes, updated.toChildren, updated.roots).isRight shouldBe true
      }
      updated
    }

  "ForestDiff" - {
    "produces no updates for identical forests" in {
      ForestDiff.diff(sample, sample) shouldBe empty
    }

    "adds a root" in checkDiff(sample, build(roots = List(1, 5, 9), 1 -> List(2, 3, 4)))

    "adds a leaf under a middle parent" in checkDiff(
      build(roots = List(1), 1 -> List(2, 3, 4), 3 -> List(5)),
      build(roots = List(1), 1 -> List(2, 3, 4), 3 -> List(5, 9))
    )

    "restores a removed middle child to its position" in checkDiff(
      build(roots = List(1, 5), 1 -> List(2, 4)),
      sample
    )

    "removes a subtree" in checkDiff(sample, build(roots = List(5)))

    "restores a removed subtree" in {
      val updates = ForestDiff.diff(build(roots = List(5)), sample)
      updates.collect { case AddLink(child, parent) => (child, parent) } should have size 3
      checkDiff(build(roots = List(5)), sample, updates)
    }

    "moves a node between parents" in checkDiff(sample, build(roots = List(1, 5), 1 -> List(2, 4), 5 -> List(3)))

    "moves a node under its former descendant" in checkDiff(
      build(roots = List(1), 1 -> List(2), 2 -> List(3)),
      build(roots = List(3), 3 -> List(1), 1 -> List(2))
    )

    "swaps a parent and its child" in checkDiff(
      build(roots = List(1), 1 -> List(2), 2 -> List(3)),
      build(roots = List(2), 2 -> List(1), 1 -> List(3))
    )

    "reorders roots" in checkDiff(sample, build(roots = List(5, 1), 1 -> List(2, 3, 4)))

    "changes data" in {
      val updated = ForestResolver.resolve(sample, UpdateData(3, 'z'))
      ForestDiff.diff(sample, updated) shouldEqual List(UpdateData(3, 'z'))
    }

    "fails when the scope is missing a list that changes" in {
      val moved = build(roots = List(1, 5), 1 -> List(2, 4), 5 -> List(3))
      ForestDiff.diff(sample, moved, Touched(Set(3), Set(Some(1)))).isLeft shouldBe true
    }

    "fails when the scope would orphan a node" in {
      val removed = build(roots = List(5))
      ForestDiff.diff(sample, removed, Touched(Set(1), Set(None, Some(1)))).isLeft shouldBe true
    }

    "for generated forests" - {
      val pairs = for {
        ops1 <- opsGen
        ops2 <- opsGen
        related <- Gen.prob(0.5)
      } yield {
        val from = resolveAll(ops1)
        (from, if (related) resolveAll(from, ops2) else resolveAll(ops2))
      }

      "produces valid updates that reach the target" in forAll(pairs)((from, to) => checkDiff(from, to))

      "is mirrored by the step tree" in forAll(pairs) { (from, to) =>
        val tree = ForestUpdateConsumer[Step.ID, Step, TreeNode](from, TreeNode(_, _, _, _))
        tree.eval(ForestDiff.diff(from, to))
        treeMatches(tree, to)
      }

      "keeps step timings the same as working them out from scratch" in forAll(pairs) { (from, to) =>
        val timeKeeper = TimeKeeper(from)
        ForestDiff.diff(from, to).foreach(timeKeeper.update)
        timingsMatch(timeKeeper, TimeKeeper(to), to)
      }

      "is persisted faithfully" in forAll(pairs) { (from, to) =>
        persist(List(ForestDiff.diff(Forest.empty, from), ForestDiff.diff(from, to))) shouldEqual to
      }

      "only needs to examine the parts that the operations touched" in forAll(opsGen, opsGen) { (ops1, ops2) =>
        val from = resolveAll(ops1)
        val (to, updates) = ops2.foldLeft((from, List.empty[Update[Step.ID, Step]])) {
          case ((forest, acc), op) =>
            val updates = toUpdates(forest, op)
            (ForestResolver.resolve(forest, updates), acc ++ updates)
        }
        val scope = Touched.from(updates)

        ForestDiff.diff(from, to, scope) match {
          case Right(forwards) => checkDiff(from, to, forwards)
          case Left(error) => fail(error)
        }
        ForestDiff.diff(to, from, scope) match {
          case Right(backwards) => checkDiff(to, from, backwards)
          case Left(error) => fail(error)
        }
      }
    }
  }
}

private object ForestDiffTest {
  /** 1 has children 2, 3 and 4. 5 is a second root. */
  val sample: Forest[Int, Char] =
    build(roots = List(1, 5), 1 -> List(2, 3, 4))

  /** Each node's data is derived from its ID */
  def build(roots: List[Int], parentsToChildren: (Int, List[Int])*): Forest[Int, Char] = {
    val ids = roots ++ parentsToChildren.flatMap(_._2)
    Forest.from(ids.map(id => id -> ('a' + id).toChar).toMap, parentsToChildren.toMap, roots)
  }

  /** For comparing lists as multisets */
  def counts[A](list: List[A]): Map[A, Int] =
    list.groupMapReduce(identity)(_ => 1)(_ + _)
}
