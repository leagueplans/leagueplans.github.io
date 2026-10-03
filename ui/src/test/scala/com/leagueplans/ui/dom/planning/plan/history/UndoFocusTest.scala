package com.leagueplans.ui.dom.planning.plan.history

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.ForestHistory.Entry
import com.leagueplans.ui.model.common.forest.{Forest, ForestHistory, ForestInterpreter, ForestResolver, Touched}
import com.leagueplans.ui.model.plan.Step
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class UndoFocusTest extends AnyFreeSpec with Matchers {
  private val stepA = Step("a")
  private val stepB = Step("b")
  private val stepC = Step("c")
  private val stepD = Step("d")
  private val stepE = Step("e")

  //  A
  //  ├ B
  //  ├ C
  //  └ D
  //  E
  private val initial: Forest[Step.ID, Step] =
    Forest.from(
      List(stepA, stepB, stepC, stepD, stepE).map(step => step.id -> step).toMap,
      Map(stepA.id -> List(stepB.id, stepC.id, stepD.id)),
      List(stepA.id, stepE.id)
    )

  /** The entry, and the forest after the change */
  private def change(
    f: ForestInterpreter[Step.ID, Step] => List[Update[Step.ID, Step]]
  ): (Entry[Step.ID, Step], Forest[Step.ID, Step]) = {
    val updates = f(ForestInterpreter(initial))
    val updated = ForestResolver.resolve(initial, updates)
    val history = ForestHistory.empty[Step.ID, Step](maxEntries = 1, maxWeight = Int.MaxValue)
      .record(initial, updated, Touched.from(updates))
    (history.undoStack.head, updated)
  }

  /** The focus after undoing the change */
  private def afterUndo(focus: Option[Step]): (ForestInterpreter[Step.ID, Step] => List[Update[Step.ID, Step]]) => Option[Step.ID] =
    f => UndoFocus.target(change(f)._1, initial, focus.map(_.id))

  /** The focus after redoing the change */
  private def afterRedo(focus: Option[Step]): (ForestInterpreter[Step.ID, Step] => List[Update[Step.ID, Step]]) => Option[Step.ID] =
    f => {
      val (entry, updated) = change(f)
      UndoFocus.target(entry, updated, focus.map(_.id))
    }

  "UndoFocus" - {
    "Undoing a deletion focuses the restored step" in {
      afterUndo(focus = None)(_.remove(stepA.id)) shouldEqual Some(stepA.id)
      afterUndo(focus = Some(stepE))(_.remove(stepC.id)) shouldEqual Some(stepC.id)
    }

    "Redoing a deletion focuses the parent" in {
      afterRedo(focus = None)(_.remove(stepC.id)) shouldEqual Some(stepA.id)
    }

    "Undoing an addition focuses the parent" in {
      afterUndo(focus = None)(_.add(Step("new"), stepA.id)) shouldEqual Some(stepA.id)
    }

    "Undoing the addition of a root" - {
      val newStep = Step("new")

      "clears the focus if it was on the step" in {
        afterUndo(focus = Some(newStep))(_.add(newStep)) shouldEqual None
      }

      "keeps the focus if it was elsewhere" in {
        afterUndo(focus = Some(stepC))(_.add(newStep)) shouldEqual Some(stepC.id)
      }
    }

    "Undoing an edit focuses the edited step" in {
      afterUndo(focus = Some(stepB))(_.update(stepD.id, _.deepCopy(repetitions = 2))) shouldEqual Some(stepD.id)
    }

    "Undoing a move focuses the moved step" in {
      afterUndo(focus = None)(_.move(stepE.id, Some(stepC.id))) shouldEqual Some(stepE.id)
    }

    "Undoing a reordering" - {
      "keeps the focus on a reordered step" in {
        afterUndo(focus = Some(stepC))(_.reorder(List(stepC.id, stepB.id, stepD.id))) shouldEqual Some(stepC.id)
        afterRedo(focus = Some(stepC))(_.reorder(List(stepC.id, stepB.id, stepD.id))) shouldEqual Some(stepC.id)
      }

      "otherwise focuses the first reordered step" in {
        afterUndo(focus = None)(_.reorder(List(stepB.id, stepD.id, stepC.id))) shouldEqual Some(stepC.id)
        afterUndo(focus = Some(stepE))(_.reorder(List(stepB.id, stepD.id, stepC.id))) shouldEqual Some(stepC.id)
      }
    }
  }
}
