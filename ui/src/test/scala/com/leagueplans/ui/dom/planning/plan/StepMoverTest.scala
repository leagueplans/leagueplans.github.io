package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class StepMoverTest extends AnyFreeSpec with Matchers {
  private val stepA = Step("a")
  private val stepB = Step("b")
  private val stepC = Step("c")
  private val stepD = Step("d")
  private val stepE = Step("e")

  //  A
  //  ├ B
  //  │ └ C
  //  └ D
  //  E
  private val initial: Forest[Step.ID, Step] =
    Forest.from(
      List(stepA, stepB, stepC, stepD, stepE).map(step => step.id -> step).toMap,
      Map(stepA.id -> List(stepB.id, stepD.id), stepB.id -> List(stepC.id)),
      List(stepA.id, stepE.id)
    )

  /** Runs the move both directly and from within an event handler, where Airstream
    * defers the resulting forest updates until the handler completes */
  private def test(move: StepMover => Unit)(
    expectedToChildren: Map[Step, List[Step]],
    expectedRoots: List[Step]
  ): Assertion = {
    val expected = (
      expectedToChildren.map((parent, children) => parent.id -> children.map(_.id)),
      expectedRoots.map(_.id)
    )

    List[(StepMover, EventBus[Unit]) => Unit](
      (mover, _) => move(mover),
      (_, bus) => bus.emit(())
    ).foreach { trigger =>
      Using(new ManualOwner) { owner =>
        val forester = Forester(initial, Observer.empty)
        forester.signal.foreach(_ => ())(using owner)
        val mover = StepMover(forester)
        val bus = EventBus[Unit]()
        bus.events.foreach(_ => move(mover))(using owner)

        trigger(mover, bus)

        val forest = forester.signal.now()
        (forest.toChildren.filter((_, children) => children.nonEmpty), forest.roots) shouldEqual expected
      }(using _.killSubscriptions()).get
    }
    succeed
  }

  private def reportedMoves(move: StepMover => Unit): List[Step.ID] =
    Using(new ManualOwner) { owner =>
      val mover = StepMover(Forester(initial, Observer.empty))
      var moves = List.empty[Step.ID]
      mover.moves.foreach(step => moves :+= step)(using owner)
      move(mover)
      moves
    }(using _.killSubscriptions()).get

  "StepMover" - {
    "reports the steps it moves" in {
      reportedMoves(_.moveUp(stepD.id)) shouldEqual List(stepD.id)
      reportedMoves(_.moveDown(stepB.id)) shouldEqual List(stepB.id)
      reportedMoves(_.indent(stepD.id)) shouldEqual List(stepD.id)
      reportedMoves(_.outdent(stepC.id)) shouldEqual List(stepC.id)
    }

    "does not report moves that don't happen" in {
      reportedMoves(_.moveUp(stepB.id)) shouldEqual List.empty
      reportedMoves(_.moveDown(stepE.id)) shouldEqual List.empty
      reportedMoves(_.indent(stepB.id)) shouldEqual List.empty
      reportedMoves(_.outdent(stepA.id)) shouldEqual List.empty
    }

    "moveUp" - {
      "swaps a step with its previous sibling" in test(_.moveUp(stepD.id))(
        Map(stepA -> List(stepD, stepB), stepB -> List(stepC)),
        List(stepA, stepE)
      )

      "swaps a root with the previous root" in test(_.moveUp(stepE.id))(
        Map(stepA -> List(stepB, stepD), stepB -> List(stepC)),
        List(stepE, stepA)
      )

      "does nothing to the first sibling" in test(_.moveUp(stepB.id))(
        Map(stepA -> List(stepB, stepD), stepB -> List(stepC)),
        List(stepA, stepE)
      )
    }

    "moveDown" - {
      "swaps a step with its next sibling" in test(_.moveDown(stepB.id))(
        Map(stepA -> List(stepD, stepB), stepB -> List(stepC)),
        List(stepA, stepE)
      )

      "does nothing to the last sibling" in test(_.moveDown(stepE.id))(
        Map(stepA -> List(stepB, stepD), stepB -> List(stepC)),
        List(stepA, stepE)
      )
    }

    "indent" - {
      "makes a step the last substep of its previous sibling" in test(_.indent(stepD.id))(
        Map(stepA -> List(stepB), stepB -> List(stepC, stepD)),
        List(stepA, stepE)
      )

      "makes a root a substep of the previous root" in test(_.indent(stepE.id))(
        Map(stepA -> List(stepB, stepD, stepE), stepB -> List(stepC)),
        List(stepA)
      )

      "does nothing to the first sibling" in test(_.indent(stepB.id))(
        Map(stepA -> List(stepB, stepD), stepB -> List(stepC)),
        List(stepA, stepE)
      )
    }

    "outdent" - {
      "places a step directly after its parent" in test(_.outdent(stepC.id))(
        Map(stepA -> List(stepB, stepC, stepD)),
        List(stepA, stepE)
      )

      "places a substep of a root directly after that root" in test(_.outdent(stepB.id))(
        Map(stepA -> List(stepD), stepB -> List(stepC)),
        List(stepA, stepB, stepE)
      )

      "does nothing to a root" in test(_.outdent(stepA.id))(
        Map(stepA -> List(stepB, stepD), stepB -> List(stepC)),
        List(stepA, stepE)
      )
    }
  }
}
