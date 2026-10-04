package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Observer
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class NewStepPositionTest extends AnyFreeSpec with Matchers {
  private val stepA = Step("a")
  private val stepB = Step("b")
  private val stepC = Step("c")
  private val stepD = Step("d")
  private val stepE = Step("e")
  private val added = Step("new")

  //  A
  //  ├ B
  //  │ └ C
  //  └ D
  //  E
  private val forest: Forest[Step.ID, Step] =
    Forest.from(
      List(stepA, stepB, stepC, stepD, stepE).map(step => step.id -> step).toMap,
      Map(stepA.id -> List(stepB.id, stepD.id), stepB.id -> List(stepC.id)),
      List(stepA.id, stepE.id)
    )

  private def at(parent: Step, index: Int) = NewStepPosition(Some(parent.id), index)
  private def atTop(index: Int) = NewStepPosition(None, index)

  "lastIn" - {
    "goes after the parent's last substep" in {
      NewStepPosition.lastIn(Some(stepA.id), forest) shouldEqual at(stepA, 2)
    }

    "goes first in a step without substeps" in {
      NewStepPosition.lastIn(Some(stepD.id), forest) shouldEqual at(stepD, 0)
    }

    "goes at the end of the plan without a parent" in {
      NewStepPosition.lastIn(None, forest) shouldEqual atTop(2)
    }
  }

  "indent" - {
    "makes it the last substep of the step above it" in {
      // Between B and D, so B is above it
      NewStepPosition.indent(at(stepA, 1), forest) shouldEqual Some(at(stepB, 1))
    }

    "does nothing when it's first among its siblings" in {
      NewStepPosition.indent(at(stepA, 0), forest) shouldEqual None
    }
  }

  "outdent" - {
    "moves it to just after its parent" in {
      NewStepPosition.outdent(at(stepB, 1), forest) shouldEqual Some(at(stepA, 1))
    }

    "moves it to the top level, just after a top-level parent" in {
      NewStepPosition.outdent(at(stepA, 2), forest) shouldEqual Some(atTop(1))
    }

    "does nothing at the top level" in {
      NewStepPosition.outdent(atTop(1), forest) shouldEqual None
    }
  }

  "insert" - {
    def inserted(position: NewStepPosition, in: Forest[Step.ID, Step] = forest): Forest[Step.ID, Step] =
      Using(new ManualOwner) { owner =>
        val forester = Forester(in, Observer.empty)
        forester.signal.foreach(_ => ())(using owner)
        forester.batch(NewStepPosition.insert(added, position, _))
        forester.signal.now()
      }(using _.killSubscriptions()).get

    "adds it among the parent's substeps at the index" in {
      inserted(at(stepA, 1)).toChildren(stepA.id) shouldEqual List(stepB.id, added.id, stepD.id)
    }

    "adds it after the last substep" in {
      inserted(at(stepA, 2)).toChildren(stepA.id) shouldEqual List(stepB.id, stepD.id, added.id)
    }

    "adds it among the top-level steps" in {
      inserted(atTop(1)).roots shouldEqual List(stepA.id, added.id, stepE.id)
    }

    "adds it at the end of the plan if the parent has been removed" in {
      val withoutD = Forest.from(
        List(stepA, stepB, stepC, stepE).map(step => step.id -> step).toMap,
        Map(stepA.id -> List(stepB.id), stepB.id -> List(stepC.id)),
        List(stepA.id, stepE.id)
      )
      inserted(at(stepD, 0), withoutD).roots shouldEqual List(stepA.id, stepE.id, added.id)
    }
  }
}
