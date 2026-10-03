package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.Forest.Update.*
import com.leagueplans.ui.model.common.forest.ForestDiffTest.sample
import com.leagueplans.ui.model.common.forest.ForestHistory.Result
import com.leagueplans.ui.model.common.forest.ForestPropertyTest.{opsGen, toUpdates}
import com.leagueplans.ui.model.plan.Step
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

final class ForestHistoryTest
  extends AnyFreeSpec
    with Matchers
    with ScalaCheckDrivenPropertyChecks {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 200)

  private type History = ForestHistory[Int, Char]
  private type Tree = Forest[Int, Char]

  /** Applies the updates, recording them as one change */
  private def change(history: History, forest: Tree, updates: Update[Int, Char]*): (History, Tree) = {
    val updated = ForestResolver.resolve(forest, updates.toList)
    (history.record(forest, updated, Touched.from(updates)), updated)
  }

  private def applied(result: Result[Int, Char], forest: Tree): (History, Tree) =
    result match {
      case Result.Applied(_, updates, history) => (history, ForestResolver.resolve(forest, updates))
      case other => fail(s"Expected the history to apply, but got $other")
    }

  private val noHistory: History = ForestHistory.empty(maxEntries = 200, maxWeight = Int.MaxValue)

  /** Removes 3, the middle child of 1 */
  private val (afterRemoval, removed) = change(noHistory, sample, RemoveLink(3, 1), RemoveNode(3))

  "ForestHistory" - {
    "undoes a change" in {
      val (_, undone) = applied(afterRemoval.undo(removed), removed)
      undone shouldEqual sample
    }

    "redoes an undone change" in {
      val (afterUndo, undone) = applied(afterRemoval.undo(removed), removed)
      val (_, redone) = applied(afterUndo.redo(undone), undone)
      redone shouldEqual removed
    }

    "has nothing to undo or redo when empty" in {
      noHistory.undo(sample) shouldEqual Result.NothingToDo()
      noHistory.redo(sample) shouldEqual Result.NothingToDo()
    }

    "clears the redo stack when a new change is recorded" in {
      val (afterUndo, undone) = applied(afterRemoval.undo(removed), removed)
      afterUndo.redoStack should have size 1
      val (afterEdit, _) = change(afterUndo, undone, UpdateData(5, 'z'))
      afterEdit.redoStack shouldBe empty
    }

    "undoes and redoes sequences of changes" in forAll(opsGen) { ops =>
      // Each group of operations is one change
      val (history, forests) =
        ops.grouped(3).foldLeft((ForestHistory.empty[Step.ID, Step](maxEntries = 200, maxWeight = Int.MaxValue), List(Forest.empty[Step.ID, Step]))) {
          case ((history, forests @ (forest :: _)), group) =>
            val (updated, updates) = group.foldLeft((forest, List.empty[Update[Step.ID, Step]])) {
              case ((forest, acc), op) =>
                val updates = toUpdates(forest, op)
                (ForestResolver.resolve(forest, updates), acc ++ updates)
            }
            val recorded = history.record(forest, updated, Touched.from(updates))
            (recorded, if (updated == forest) forests else updated +: forests)
          case (acc, _) => acc
        }

      history.undoStack should have size (forests.size - 1)

      val (afterUndos, undone) = forests.tail.foldLeft((history, forests.head)) {
        case ((history, forest), expected) =>
          history.undo(forest) match {
            case Result.Applied(_, updates, history) =>
              val undone = ForestResolver.resolve(forest, updates)
              undone shouldEqual expected
              (history, undone)
            case other => fail(s"Expected the history to apply, but got $other")
          }
      }
      afterUndos.undo(undone) shouldEqual Result.NothingToDo()

      forests.reverse.tail.foldLeft((afterUndos, undone)) {
        case ((history, forest), expected) =>
          history.redo(forest) match {
            case Result.Applied(_, updates, history) =>
              val redone = ForestResolver.resolve(forest, updates)
              redone shouldEqual expected
              (history, redone)
            case other => fail(s"Expected the history to apply, but got $other")
          }
      }
      succeed
    }

    "only records net changes" - {
      "ignoring a node that was added and removed" in {
        val (history, _) = change(noHistory, sample, AddNode(9, 'j'), AddLink(9, 5), RemoveLink(9, 5), RemoveNode(9))
        history shouldEqual noHistory
      }

      "ignoring a list that was reordered and restored" in {
        val (history, _) = change(noHistory, sample, Reorder(List(4, 3, 2), Some(1)), Reorder(List(2, 3, 4), Some(1)))
        history shouldEqual noHistory
      }

      "ignoring the roots when a node is added under a parent" in {
        val (history, _) = change(noHistory, sample, AddNode(9, 'j'), AddLink(9, 5))
        history.undoStack.map(_.touched) shouldEqual List(Touched(Set(9), Set(Some(9), Some(5))))
      }
    }

    "drops the oldest entries past the limit" in {
      val (history, _) = (0 until 205).foldLeft((noHistory, sample)) { case ((history, forest), i) =>
        change(history, forest, UpdateData(5, (i + 1000).toChar))
      }
      history.undoStack should have size 200
      history.undoStack.last.after.nodes(5) shouldEqual Some(((5 + 1000).toChar, None))
    }

    "drops the oldest entries past the weight limit" in {
      // Each edit touches one node, and so weighs 20
      val (history, _) = (0 until 10).foldLeft((noHistory.copy(maxWeight = 100), sample)) {
        case ((history, forest), i) => change(history, forest, UpdateData(5, (i + 1000).toChar))
      }
      history.undoStack.map(_.after.nodes(5).map(_._1)) shouldEqual (5 until 10).reverse.map(i => Some((i + 1000).toChar))
    }

    "keeps the most recent entry, however heavy" in {
      val (history, _) = change(noHistory.copy(maxWeight = 1), sample, RemoveLink(3, 1), RemoveNode(3))
      history.undoStack should have size 1
    }

    "on conflict" - {
      "refuses to undo when a touched node has changed since" in {
        val (history, forest) = change(noHistory, sample, UpdateData(3, 'z'))
        val remote = ForestResolver.resolve(forest, UpdateData(3, 'y'))
        history.undo(remote) match {
          case Result.Conflict(_, history) => history shouldEqual noHistory
          case other => fail(s"Expected a conflict, but got $other")
        }
      }

      "refuses to undo when a touched list has changed since" in {
        val remote = ForestResolver.resolve(removed, List(AddNode(9, 'j'), AddLink(9, 1)))
        afterRemoval.undo(remote) shouldBe a[Result.Conflict[?, ?]]
      }

      "refuses to redo when a touched list has changed since" in {
        val (afterUndo, undone) = applied(afterRemoval.undo(removed), removed)
        val remote = ForestResolver.resolve(undone, Reorder(List(4, 3, 2), Some(1)))
        afterUndo.redo(remote) match {
          case Result.Conflict(_, history) => history.redoStack shouldBe empty
          case other => fail(s"Expected a conflict, but got $other")
        }
      }

      "keeps changes made elsewhere" in {
        val remote = ForestResolver.resolve(removed, List(UpdateData(2, 'y'), AddNode(9, 'j'), AddLink(9, 5)))
        val (_, undone) = applied(afterRemoval.undo(remote), remote)
        undone shouldEqual ForestResolver.resolve(sample, List(UpdateData(2, 'y'), AddNode(9, 'j'), AddLink(9, 5)))
      }

      "can still undo an older change that doesn't conflict" in {
        val (history, forest) = change(afterRemoval, removed, UpdateData(5, 'z'))
        val remote = ForestResolver.resolve(forest, UpdateData(5, 'y'))
        val afterConflict = history.undo(remote) match {
          case Result.Conflict(_, history) => history
          case other => fail(s"Expected a conflict, but got $other")
        }
        val (_, undone) = applied(afterConflict.undo(remote), remote)
        undone shouldEqual ForestResolver.resolve(sample, UpdateData(5, 'y'))
      }
    }
  }
}
