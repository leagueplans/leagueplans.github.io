package com.leagueplans.ui.integration

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.forest.Forester.HistoryOutcome
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.plan.{Duration, Plan, Step}
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.storage.model.{PlanMetadata, StepUpdates}
import com.leagueplans.ui.storage.opfs.PlanDirectory
import com.leagueplans.uicommon.utils.airstream.ObservableOps.flatMapConcat
import com.leagueplans.uicommon.wrappers.opfs.{FileSystemError, MockDirectoryHandle}
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.{ManualOwner, Owner}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues, OptionValues}

import scala.annotation.nowarn
import scala.collection.mutable.ListBuffer
import scala.util.Using

final class ForesterUndoIntegrationTest
  extends AnyFreeSpec
    with Matchers
    with EitherValues
    with OptionValues {

  private val stepA = Step("a")
  private val stepB = Step("b")
  private val stepC = Step("c")
  private val stepD = Step("d")
  private val stepE = Step("e")
  private val stepF = Step("f")
  private val stepG = Step("g")

  //  A
  //  ├ B
  //  │ └ C
  //  ├ D
  //  └ F
  //  E
  //  G
  private val initial: Forest[Step.ID, Step] =
    Forest.from(
      List(stepA, stepB, stepC, stepD, stepE, stepF, stepG).map(step => step.id -> step).toMap,
      Map(stepA.id -> List(stepB.id, stepD.id, stepF.id), stepB.id -> List(stepC.id)),
      List(stepA.id, stepE.id, stepG.id)
    )

  /** A forester whose batches are persisted to a mock plan directory */
  private final class Harness(using owner: Owner) {
    val external: ListBuffer[List[Update[Step.ID, Step]]] = ListBuffer.empty
    val forester: Forester[Step.ID, Step] = Forester(initial, Observer(external += _))
    val outcomes: ListBuffer[(HistoryOutcome[Step.ID, Step], Forest[Step.ID, Step])] = ListBuffer.empty
    private val directory = PlanDirectory(new MockDirectoryHandle)
    private val deferred = EventBus[() => Unit]()

    forester.signal.foreach(_ => ())
    directory
      .create(PlanMetadata("test"), Plan("test", initial, Armageddon.settings))
      .foreach(_ => ())
    forester
      .updates
      .map(StepUpdates(_))
      .flatMapConcat(directory.applyUpdate)
      .foreach(_ => ()): @nowarn("msg=discarded non-Unit value")
    forester.historyOutcomes.withCurrentValueOf(forester.signal).foreach(outcomes += _)
    deferred.events.foreach(_())

    /** Runs the action from within an event handler, where Airstream defers forest updates */
    def runDeferred(action: => Unit): Unit =
      deferred.emit(() => action)

    def check(expected: Forest[Step.ID, Step]): Assertion = {
      withClue("In memory:")(forester.signal.now() shouldEqual expected)
      withClue("Persisted:")(persisted shouldEqual expected)
    }

    private def persisted: Forest[Step.ID, Step] = {
      var eventualResult = Option.empty[Either[FileSystemError, Plan]]
      directory.readPlan().foreach(result => eventualResult = Some(result))
      eventualResult.value.value.steps
    }
  }

  private def withHarness(f: Harness => Assertion): Assertion =
    Using(new ManualOwner)(owner =>
      f(Harness(using owner))
    )(using _.killSubscriptions()).get

  /** Checks the edit, its undo and its redo, both run directly and run from event handlers */
  private def roundTrip(edit: Forester[Step.ID, Step] => Unit): Assertion = {
    List(false, true).foreach(deferred =>
      withClue(if (deferred) "Deferred:" else "Direct:")(withHarness { harness =>
        def run(action: => Unit): Unit =
          if (deferred) harness.runDeferred(action) else action

        run(edit(harness.forester))
        val edited = harness.forester.signal.now()
        edited should not equal initial
        harness.check(edited)

        run(harness.forester.undo())
        withClue("After undoing:")(harness.check(initial))

        run(harness.forester.redo())
        withClue("After redoing:")(harness.check(edited))
      })
    )
    succeed
  }

  "Undo and redo" - {
    "Adding" - {
      "a root step" in roundTrip(_.add(Step("new")))
      "a substep" in roundTrip(_.add(Step("new"), stepB.id))
    }

    "Removing" - {
      "a leaf" in roundTrip(_.remove(stepC.id))
      "the middle child of a parent" in roundTrip(_.remove(stepD.id))
      "a root with several levels of substeps" in roundTrip(_.remove(stepA.id))
      "a root between other roots" in roundTrip(_.remove(stepE.id))
    }

    "Moving" - {
      "into a parent" in roundTrip(_.move(stepE.id, stepB.id))
      "out to the roots" in roundTrip(_.promoteToRoot(stepC.id))
      "into a different parent" in roundTrip(_.move(stepC.id, stepD.id))
      "and reordering in one batch" in roundTrip(_.batch { batch =>
        batch.move(stepE.id, stepA.id)
        batch.reorder(List(stepE.id, stepB.id, stepD.id, stepF.id))
      })
    }

    "Pasting a subtree in one batch" in roundTrip(_.batch { batch =>
      val root = Step("pasted")
      val children = List(Step("child 1"), Step("child 2"))
      val grandchildren = List(Step("grandchild 1"), Step("grandchild 2"))
      batch.add(root, stepD.id)
      children.foreach(batch.add(_, root.id))
      grandchildren.foreach(batch.add(_, children.head.id))
    })

    "Updating" - {
      "a description" in roundTrip(_.update(stepD.id, _.deepCopy(description = "updated")))
      "a duration" in roundTrip(_.update(stepD.id, _.deepCopy(duration = Duration.ticks(10))))
      "repetitions" in roundTrip(_.update(stepD.id, _.deepCopy(repetitions = 3)))
    }

    "Reordering" - {
      "roots" in roundTrip(_.reorder(List(stepG.id, stepA.id, stepE.id)))
      "children" in roundTrip(_.reorder(List(stepF.id, stepB.id, stepD.id)))
    }

    "Separate batches are undone separately" in withHarness { harness =>
      val first = Step("first")
      harness.forester.add(first)
      val afterFirst = harness.forester.signal.now()
      harness.forester.add(Step("second"))

      harness.forester.undo()
      harness.check(afterFirst)
      harness.forester.undo()
      harness.check(initial)
    }

    "Each undo and redo is saved as one batch" in withHarness { harness =>
      harness.forester.remove(stepA.id)
      harness.forester.undo()
      harness.forester.redo()
      harness.external should have size 3
      all(harness.external) should not be empty
    }

    "Undoing with nothing to undo does nothing" in withHarness { harness =>
      harness.forester.undo()
      harness.forester.redo()
      harness.external shouldBe empty
      harness.outcomes shouldBe empty
      harness.check(initial)
    }

    "Injected batches aren't recorded" in withHarness { harness =>
      harness.forester.inject(List(Update.UpdateData(stepD.id, stepD.deepCopy(description = "remote"))))
      harness.forester.historyStatus.now().nextUndo shouldBe None
    }

    "The history status shows the next entries" in withHarness { harness =>
      harness.forester.remove(stepC.id)
      val status = harness.forester.historyStatus.now()
      status.nextUndo.map(_.touched.nodes) shouldEqual Some(Set(stepC.id))
      status.nextRedo shouldBe None

      harness.forester.undo()
      val afterUndo = harness.forester.historyStatus.now()
      afterUndo.nextUndo shouldBe None
      afterUndo.nextRedo.map(_.touched.nodes) shouldEqual Some(Set(stepC.id))
    }

    "Outcomes are observed alongside the updated forest" in withHarness { harness =>
      harness.forester.remove(stepD.id)
      val edited = harness.forester.signal.now()
      harness.runDeferred(harness.forester.undo())
      harness.forester.redo()
      harness.outcomes.toList should matchPattern {
        case List((HistoryOutcome.Undone(_), `initial`), (HistoryOutcome.Redone(_), `edited`)) =>
      }
    }

    "With changes from elsewhere" - {
      "keeps changes that don't conflict" in withHarness { harness =>
        harness.forester.remove(stepD.id)
        val remoteB = stepB.deepCopy(description = "remote")
        harness.forester.inject(List(Update.UpdateData(stepB.id, remoteB)))
        harness.forester.undo()
        harness.check(Forest.from(
          initial.nodes + (stepB.id -> remoteB),
          initial.toChildren,
          initial.roots
        ))
      }

      "refuses to undo a conflicting change" in withHarness { harness =>
        harness.forester.update(stepD.id, _.deepCopy(description = "local"))
        val remote = Forest.from(
          initial.nodes + (stepD.id -> stepD.deepCopy(description = "remote")),
          initial.toChildren,
          initial.roots
        )
        harness.forester.inject(List(Update.UpdateData(stepD.id, remote.nodes(stepD.id))))
        harness.forester.undo()

        harness.check(remote)
        harness.outcomes.map(_._1).toList should matchPattern { case List(HistoryOutcome.UndoConflict(_)) => }
        harness.forester.historyStatus.now().nextUndo shouldBe None
      }

      "refuses to redo a conflicting change" in withHarness { harness =>
        harness.forester.remove(stepD.id)
        harness.forester.undo()
        harness.forester.inject(List(Update.Reorder(List(stepF.id, stepD.id, stepB.id), Some(stepA.id))))
        val remote = harness.forester.signal.now()
        harness.forester.redo()

        harness.check(remote)
        harness.outcomes.map(_._1).toList.last should matchPattern { case HistoryOutcome.RedoConflict(_) => }
        harness.forester.historyStatus.now().nextRedo shouldBe None
      }
    }
  }
}
