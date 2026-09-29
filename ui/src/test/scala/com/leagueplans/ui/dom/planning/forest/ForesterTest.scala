package com.leagueplans.ui.dom.planning.forest

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.plan.Step
import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.util.Using

final class ForesterTest extends AnyFreeSpec with Matchers {
  private val parent = Step("parent")
  private val child = Step("child")

  private type Batches = List[List[Update[Step.ID, Step]]]

  /** Runs the operations, returning the resulting forest, the batches the forester emitted, and
    * the batches its external observer received */
  private def run(operations: Forester[Step.ID, Step] => Unit): (Forest[Step.ID, Step], Batches, Batches) =
    Using(new ManualOwner) { owner =>
      var external = List.empty[List[Update[Step.ID, Step]]]
      var emitted = List.empty[List[Update[Step.ID, Step]]]
      val forester = Forester(Forest.empty[Step.ID, Step], Observer(batch => external :+= batch))
      forester.signal.foreach(_ => ())(using owner)
      forester.updates.foreach(batch => emitted :+= batch)(using owner)

      operations(forester)
      (forester.signal.now(), emitted, external)
    }(using _.killSubscriptions()).get

  private def isChildOfParent(forest: Forest[Step.ID, Step]): Assertion = {
    forest.toParent.get(child.id) shouldEqual Some(parent.id)
    forest.roots shouldEqual List(parent.id)
  }

  "Forester" - {
    "batch" - {
      "emits the updates of all its operations as a single batch" in {
        val (_, emitted, external) = run(_.batch { batch =>
          batch.add(parent)
          batch.add(child, parent.id)
        })

        emitted shouldEqual List(List(
          Update.AddNode(parent.id, parent),
          Update.AddNode(child.id, child),
          Update.AddLink(child.id, parent.id)
        ))
        external shouldEqual emitted
      }

      "applies each operation to the forest left by the previous ones" in {
        val (forest, _, _) = run(_.batch { batch =>
          batch.add(parent)
          batch.forest.contains(parent.id) shouldBe true
          batch.add(child, parent.id)
        })

        isChildOfParent(forest)
      }

      "emits nothing when its operations don't change the forest" in {
        val (_, emitted, external) = run { forester =>
          forester.add(parent)
          forester.batch { batch =>
            batch.remove(child.id)
            batch.update(parent)
          }
        }

        emitted should have size 1
        external should have size 1
      }

      "runs against the result of earlier batches when called from an event handler" in {
        val (forest, emitted, _) = run { forester =>
          val bus = EventBus[Unit]()
          Using(new ManualOwner) { owner =>
            bus.events.foreach { _ =>
              forester.batch(_.add(parent))
              forester.batch(_.add(child, parent.id))
            }(using owner)
            bus.emit(())
          }(using _.killSubscriptions()).get
        }

        isChildOfParent(forest)
        emitted should have size 2
      }
    }
  }
}
