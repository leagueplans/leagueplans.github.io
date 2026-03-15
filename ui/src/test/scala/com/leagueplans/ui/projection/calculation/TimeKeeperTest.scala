package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.plan.{Duration as StepDuration, EffectList, Step, StepDetails}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{Duration, FiniteDuration}

final class TimeKeeperTest extends AnyFreeSpec with Matchers {

  // ---- helpers ----

  private def step(id: String, reps: Int = 1, dur: StepDuration = StepDuration.ticks(0)): Step =
    Step(Step.ID.fromString(id), StepDetails("", EffectList(List.empty), List.empty, reps, dur))

  private def forest(
    nodes: Map[Step.ID, Step],
    parentsToChildren: Map[Step.ID, List[Step.ID]] = Map.empty,
    roots: List[Step.ID]
  ): Forest[Step.ID, Step] =
    Forest.from(nodes, parentsToChildren, roots)

  private def keeper(f: Forest[Step.ID, Step]): TimeKeeper = TimeKeeper(f)

  /** ticks(n) in scala Duration, matching StepDuration.ticks(n).asScala */
  private def ticks(n: Int): FiniteDuration = StepDuration.ticks(n).asScala

  "initial state" - {

    "empty forest has endTime zero" in {
      keeper(Forest.empty).endTime.now() shouldBe Duration.Zero
    }

    "single step without duration" - {
      val a = step("a")
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))

      "start is None" in { k.get(a.id).now().start shouldBe None }
      "finish is None" in { k.get(a.id).now().finish shouldBe None }
      "endTime is zero" in { k.endTime.now() shouldBe Duration.Zero }
    }

    "a single step with duration anchors at Duration.Zero" - {
      val a = step("a", dur = StepDuration.ticks(3))
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))

      "start is Duration.Zero" in { k.get(a.id).now().start shouldBe Some(Duration.Zero) }
      "finish is the step's duration" in { k.get(a.id).now().finish shouldBe Some(ticks(3)) }
      "endTime equals the step's duration" in { k.endTime.now() shouldBe ticks(3) }
    }

    "two timed roots run sequentially" - {
      val a = step("a", dur = StepDuration.ticks(3))
      val b = step("b", dur = StepDuration.ticks(5))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))

      "first root anchors at zero" in { k.get(a.id).now().start shouldBe Some(Duration.Zero) }
      "second root starts at first root's finish" in { k.get(b.id).now().start shouldBe Some(ticks(3)) }
      "endTime is sum of both durations" in { k.endTime.now() shouldBe ticks(8) }
    }

    "untimed root before timed root" - {
      val a = step("a")
      val b = step("b", dur = StepDuration.ticks(5))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))

      "untimed root has start = None" in { k.get(a.id).now().start shouldBe None }
      "timed root is the clock anchor at Duration.Zero" in { k.get(b.id).now().start shouldBe Some(Duration.Zero) }
      "endTime equals only the timed root's duration" in { k.endTime.now() shouldBe ticks(5) }
    }

    "child starts after parent's own step" - {
      val parent = step("p", dur = StepDuration.ticks(2))
      val child  = step("c", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      ))

      "parent anchors at zero" in { k.get(parent.id).now().start shouldBe Some(Duration.Zero) }
      "child starts at parent's stepDuration offset" in { k.get(child.id).now().start shouldBe Some(ticks(2)) }
      "parent totalChildDuration equals child durationPerParentRep" in {
        k.get(parent.id).now().totalChildDuration shouldBe Some(ticks(3))
      }
      "parent durationPerParentRep spans own step plus child" in {
        k.get(parent.id).now().durationPerParentRep shouldBe Some(ticks(5))
      }
      "endTime equals parent's full durationPerParentRep" in { k.endTime.now() shouldBe ticks(5) }
    }

    "two sequential children" - {
      val parent = step("p", dur = StepDuration.ticks(1))
      val c1     = step("c1", dur = StepDuration.ticks(2))
      val c2     = step("c2", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, c1.id -> c1, c2.id -> c2),
        parentsToChildren = Map(parent.id -> List(c1.id, c2.id)),
        roots = List(parent.id)
      ))

      "c1 starts at parent's stepDuration" in { k.get(c1.id).now().start shouldBe Some(ticks(1)) }
      "c2 starts at c1's finish" in { k.get(c2.id).now().start shouldBe Some(ticks(3)) }
      "parent totalChildDuration = c1 + c2" in {
        k.get(parent.id).now().totalChildDuration shouldBe Some(ticks(5))
      }
    }

    "step with repetitions multiplies durationPerParentRep" - {
      val a = step("a", reps = 3, dur = StepDuration.ticks(4))
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))

      "durationPerParentRep = durationPerRep × reps" in { k.get(a.id).now().durationPerParentRep shouldBe Some(ticks(12)) }
      "endTime reflects repetitions" in { k.endTime.now() shouldBe ticks(12) }
    }

    "child inside a loop (parent reps > 1)" - {
      val parent = step("p", reps = 3, dur = StepDuration.ticks(2))
      val child  = step("c", dur = StepDuration.ticks(1))
      val k = keeper(forest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      ))

      "child start is parent.start + parent.stepDuration" in { k.get(child.id).now().start shouldBe Some(ticks(2)) }
      "child finish is child.start + child.durationPerParentRep" in { k.get(child.id).now().finish shouldBe Some(ticks(3)) }
      "parent start is Some(Zero) (loop itself is still scheduled)" in {
        k.get(parent.id).now().start shouldBe Some(Duration.Zero)
      }
      "parent totalChildDuration still set (used to compute durationPerParentRep)" in {
        k.get(parent.id).now().totalChildDuration shouldBe Some(ticks(1))
      }
      "parent durationPerParentRep = (own + child) × reps" in {
        k.get(parent.id).now().durationPerParentRep shouldBe Some(ticks(9))
      }
    }

    "grandchild inside a loop starts at first occurrence time" - {
      val gp    = step("gp", reps = 2, dur = StepDuration.ticks(1))
      val child = step("c",  dur = StepDuration.ticks(2))
      val gc    = step("gc", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(gp.id -> gp, child.id -> child, gc.id -> gc),
        parentsToChildren = Map(gp.id -> List(child.id), child.id -> List(gc.id)),
        roots = List(gp.id)
      ))

      "child start is gp.start + gp.stepDuration" in  { k.get(child.id).now().start shouldBe Some(ticks(1)) }
      "grandchild start is child.start + child.stepDuration" in { k.get(gc.id).now().start shouldBe Some(ticks(3)) }
    }

    "step following a loop starts at the loop's finish" - {
      val loop  = step("loop", reps = 2, dur = StepDuration.ticks(1))
      val child = step("c",   dur = StepDuration.ticks(3))
      val after = step("after", dur = StepDuration.ticks(5))
      val k = keeper(forest(
        Map(loop.id -> loop, child.id -> child, after.id -> after),
        parentsToChildren = Map(loop.id -> List(child.id)),
        roots = List(loop.id, after.id)
      ))

      "loop durationPerRep = own + child" in { k.get(loop.id).now().durationPerRep shouldBe Some(ticks(4)) }
      "loop durationPerParentRep = perRep × reps" in { k.get(loop.id).now().durationPerParentRep shouldBe Some(ticks(8)) }
      "step after loop starts at loop's finish" in { k.get(after.id).now().start shouldBe Some(ticks(8)) }
      "endTime = loop + after" in { k.endTime.now() shouldBe ticks(13) }
    }
  }

  "State.finish" - {
    "None start always yields None finish regardless of durationPerParentRep" in {
      val s = TimeKeeper.State(
        start = None,
        stepDuration = Some(StepDuration.ticks(5)),
        totalChildDuration = None,
        repetitions = 1
      )
      s.finish shouldBe None
    }

    "Some start with no durationPerParentRep yields start as finish" in {
      val s = TimeKeeper.State(start = Some(ticks(3)), stepDuration = None, totalChildDuration = None, repetitions = 1)
      s.finish shouldBe Some(ticks(3))
    }

    "Some start with durationPerParentRep yields start + durationPerParentRep" in {
      val s = TimeKeeper.State(start = Some(ticks(2)), stepDuration = Some(StepDuration.ticks(3)), totalChildDuration = None, repetitions = 1)
      s.finish shouldBe Some(ticks(5))
    }
  }

  "AddNode" - {
    "first timed step added to empty forest anchors at zero" in {
      val k = keeper(Forest.empty)
      val a = step("a", dur = StepDuration.ticks(3))
      k.update(Update.AddNode(a.id, a))
      k.get(a.id).now().start shouldBe Some(Duration.Zero)
      k.endTime.now() shouldBe ticks(3)
    }

    "first untimed step added to empty forest has start = None" in {
      val k = keeper(Forest.empty)
      val a = step("a")
      k.update(Update.AddNode(a.id, a))
      k.get(a.id).now().start shouldBe None
      k.endTime.now() shouldBe Duration.Zero
    }

    "timed step added after an existing timed root starts at that root's finish" in {
      val a = step("a", dur = StepDuration.ticks(3))
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))
      val b = step("b", dur = StepDuration.ticks(5))
      k.update(Update.AddNode(b.id, b))
      k.get(b.id).now().start shouldBe Some(ticks(3))
      k.endTime.now() shouldBe ticks(8)
    }

    "untimed step added after a timed root inherits the running start" in {
      val a = step("a", dur = StepDuration.ticks(2))
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))
      val b = step("b")
      k.update(Update.AddNode(b.id, b))
      // clock is running at ticks(2), so b.start = Some(ticks(2))
      k.get(b.id).now().start shouldBe Some(ticks(2))
    }

    "timed step with reps > 1 added to empty forest anchors at zero with full durationPerParentRep" in {
      val k = keeper(Forest.empty)
      val a = step("a", reps = 2, dur = StepDuration.ticks(3))
      k.update(Update.AddNode(a.id, a))
      k.get(a.id).now().start shouldBe Some(Duration.Zero)
      k.get(a.id).now().durationPerParentRep shouldBe Some(ticks(6))
      k.endTime.now() shouldBe ticks(6)
    }
  }

  "RemoveNode" - {
    "removing the only root resets endTime to zero" in {
      val a = step("a", dur = StepDuration.ticks(5))
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))
      k.update(Update.RemoveNode(a.id))
      k.endTime.now() shouldBe Duration.Zero
    }

    "removing the first of two timed roots: second root takes over the anchor position" in {
      val a = step("a", dur = StepDuration.ticks(3))
      val b = step("b", dur = StepDuration.ticks(5))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.RemoveNode(a.id))
      k.get(b.id).now().start shouldBe Some(Duration.Zero)
      k.endTime.now() shouldBe ticks(5)
    }

    "removing a middle root shifts the following root forward" in {
      val a = step("a", dur = StepDuration.ticks(2))
      val b = step("b", dur = StepDuration.ticks(3))
      val c = step("c", dur = StepDuration.ticks(4))
      val k = keeper(forest(Map(a.id -> a, b.id -> b, c.id -> c), roots = List(a.id, b.id, c.id)))
      k.update(Update.RemoveNode(b.id))
      k.get(c.id).now().start shouldBe Some(ticks(2))
      k.endTime.now() shouldBe ticks(6)
    }

    "removing a non-root child: following sibling shifts forward and parent's totalChildDuration updates" in {
      val parent = step("p", dur = StepDuration.ticks(1))
      val c1     = step("c1", dur = StepDuration.ticks(2))
      val c2     = step("c2", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, c1.id -> c1, c2.id -> c2),
        parentsToChildren = Map(parent.id -> List(c1.id, c2.id)),
        roots = List(parent.id)
      ))
      k.update(Update.RemoveNode(c1.id))
      // c2 takes c1's slot: parent.start(0) + parent.own(1) = ticks(1)
      k.get(c2.id).now().start shouldBe Some(ticks(1))
      k.get(parent.id).now().totalChildDuration shouldBe Some(ticks(3))
      k.endTime.now() shouldBe ticks(4)
    }
  }

  "AddLink" - {
    "linking a root as child: child start shifts from anchor to parent's finish" in {
      // b comes before a in roots, so b.start = Some(0) initially
      val a = step("a", dur = StepDuration.ticks(2))
      val b = step("b", dur = StepDuration.ticks(3))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(b.id, a.id)))
      k.get(b.id).now().start shouldBe Some(Duration.Zero)
      k.update(Update.AddLink(b.id, a.id))
      // b is now a's child: b.start = a.start(0) + a.stepDuration(2) = ticks(2)
      k.get(b.id).now().start shouldBe Some(ticks(2))
      k.get(a.id).now().totalChildDuration shouldBe Some(ticks(3))
      k.endTime.now() shouldBe ticks(5)
    }

    "second child starts at first child's finish" in {
      // c2 comes before parent in roots, so c2.start = Some(0) initially
      val parent = step("p", dur = StepDuration.ticks(1))
      val c1     = step("c1", dur = StepDuration.ticks(2))
      val c2     = step("c2", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, c1.id -> c1, c2.id -> c2),
        parentsToChildren = Map(parent.id -> List(c1.id)),
        roots = List(c2.id, parent.id)
      ))
      k.get(c2.id).now().start shouldBe Some(Duration.Zero)
      k.update(Update.AddLink(c2.id, parent.id))
      // c2 is appended as second child; starts at c1.finish = 0(parent) + 1(own) + 2(c1) = 3
      k.get(c2.id).now().start shouldBe Some(ticks(3))
    }

    "child added to a loop parent starts at parent.start + parent.stepDuration" in {
      val parent = step("p", reps = 2, dur = StepDuration.ticks(1))
      val child  = step("c", dur = StepDuration.ticks(3))
      val k = keeper(forest(Map(parent.id -> parent, child.id -> child), roots = List(parent.id, child.id)))
      k.update(Update.AddLink(child.id, parent.id))
      k.get(child.id).now().start shouldBe Some(ticks(1))
    }
  }

  "RemoveLink" - {
    "removed child becomes a new root after the parent" in {
      // child is first child; c1 is second. Initially child.start = ticks(2) (parent.own).
      // After removal, c1 stays so parent.finish = 2 + 4 = ticks(6); child is re-rooted there.
      val parent = step("p", dur = StepDuration.ticks(2))
      val child  = step("c", dur = StepDuration.ticks(3))
      val c1     = step("c1", dur = StepDuration.ticks(4))
      val k = keeper(forest(
        Map(parent.id -> parent, child.id -> child, c1.id -> c1),
        parentsToChildren = Map(parent.id -> List(child.id, c1.id)),
        roots = List(parent.id)
      ))
      k.get(child.id).now().start shouldBe Some(ticks(2))
      k.update(Update.RemoveLink(child.id, parent.id))
      // c1 shifts forward to become first child at ticks(2)
      // parent.finish = 0 + 2(own) + 4(c1) = ticks(6); child is appended after parent
      k.get(child.id).now().start shouldBe Some(ticks(6))
      k.get(parent.id).now().totalChildDuration shouldBe Some(ticks(4))
      k.endTime.now() shouldBe ticks(9)
    }

    "following sibling of the removed child shifts forward" in {
      val parent = step("p", dur = StepDuration.ticks(1))
      val c1     = step("c1", dur = StepDuration.ticks(2))
      val c2     = step("c2", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, c1.id -> c1, c2.id -> c2),
        parentsToChildren = Map(parent.id -> List(c1.id, c2.id)),
        roots = List(parent.id)
      ))
      k.update(Update.RemoveLink(c1.id, parent.id))
      // c2 now starts where c1 started (parent.start + parent.stepDuration = 0 + 1 = 1)
      k.get(c2.id).now().start shouldBe Some(ticks(1))
    }

    "untimed child detached: placed after parent with no timing cascade to parent" in {
      // Untimed child contributes no durationPerParentRep, so removing it doesn't change
      // parent's totalChildDuration — the else branch fires and places child directly.
      val parent = step("p", dur = StepDuration.ticks(3))
      val child  = step("c") // untimed
      val k = keeper(forest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      ))
      k.update(Update.RemoveLink(child.id, parent.id))
      k.get(parent.id).now().totalChildDuration shouldBe None
      // child is now a root placed after parent; parent.finish = ticks(3)
      k.get(child.id).now().start shouldBe Some(ticks(3))
      k.endTime.now() shouldBe ticks(3)
    }
  }

  "ChangeParent" - {
    "moving a child updates both old and new parents' timings" in {
      val p1 = step("p1", dur = StepDuration.ticks(1))
      val p2 = step("p2", dur = StepDuration.ticks(2))
      val c  = step("c",  dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(p1.id -> p1, p2.id -> p2, c.id -> c),
        parentsToChildren = Map(p1.id -> List(c.id)),
        roots = List(p1.id, p2.id)
      ))
      k.update(Update.ChangeParent(c.id, p1.id, p2.id))

      // p1 loses c: its totalChildDuration clears
      k.get(p1.id).now().totalChildDuration shouldBe None
      // p2 gains c: c starts at p2.start + p2.stepDuration = ticks(1) + ticks(2) = ticks(3)
      k.get(c.id).now().start shouldBe Some(ticks(3))
      k.get(p2.id).now().totalChildDuration shouldBe Some(ticks(3))
    }
  }

  "UpdateData" - {
    "increasing duration shifts following sibling" in {
      val a = step("a", dur = StepDuration.ticks(2))
      val b = step("b", dur = StepDuration.ticks(3))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.UpdateData(a.id, step("a", dur = StepDuration.ticks(5))))
      k.get(b.id).now().start shouldBe Some(ticks(5))
      k.endTime.now() shouldBe ticks(8)
    }

    "reducing duration shifts following sibling back" in {
      val a = step("a", dur = StepDuration.ticks(5))
      val b = step("b", dur = StepDuration.ticks(2))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.UpdateData(a.id, step("a", dur = StepDuration.ticks(1))))
      k.get(b.id).now().start shouldBe Some(ticks(1))
      k.endTime.now() shouldBe ticks(3)
    }

    "zeroing out a step's duration unschedules it and shifts the anchor to its successor" in {
      val a = step("a", dur = StepDuration.ticks(3))
      val b = step("b", dur = StepDuration.ticks(2))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.UpdateData(a.id, step("a")))
      // a no longer has duration, so b becomes the first anchor
      k.get(a.id).now().start shouldBe None
      k.get(b.id).now().start shouldBe Some(Duration.Zero)
      k.endTime.now() shouldBe ticks(2)
    }

    "changing repetitions updates durationPerParentRep and shifts following sibling" in {
      val a = step("a", reps = 2, dur = StepDuration.ticks(3))
      val b = step("b", dur = StepDuration.ticks(1))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.UpdateData(a.id, step("a", reps = 4, dur = StepDuration.ticks(3))))
      k.get(a.id).now().durationPerParentRep shouldBe Some(ticks(12))
      k.get(b.id).now().start shouldBe Some(ticks(12))
    }

    "updating a child's duration propagates to parent's totalChildDuration and endTime" in {
      val parent = step("p", dur = StepDuration.ticks(1))
      val child  = step("c", dur = StepDuration.ticks(2))
      val k = keeper(forest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      ))
      k.update(Update.UpdateData(child.id, step("c", dur = StepDuration.ticks(5))))
      k.get(parent.id).now().totalChildDuration shouldBe Some(ticks(5))
      k.get(parent.id).now().durationPerParentRep shouldBe Some(ticks(6))
      k.endTime.now() shouldBe ticks(6)
    }

    "increasing a parent's stepDuration shifts its children's starts" in {
      val parent = step("p", dur = StepDuration.ticks(2))
      val child  = step("c", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, child.id -> child),
        parentsToChildren = Map(parent.id -> List(child.id)),
        roots = List(parent.id)
      ))
      k.get(child.id).now().start shouldBe Some(ticks(2))
      k.update(Update.UpdateData(parent.id, step("p", dur = StepDuration.ticks(5))))
      k.get(child.id).now().start shouldBe Some(ticks(5))
    }

    "no-op update (same duration) leaves state unchanged" in {
      val a = step("a", dur = StepDuration.ticks(3))
      val b = step("b", dur = StepDuration.ticks(2))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.UpdateData(a.id, step("a", dur = StepDuration.ticks(3))))
      k.get(b.id).now().start shouldBe Some(ticks(3))
    }
  }

  "Reorder" - {
    "swapping two timed roots reverses their schedule positions" in {
      val a = step("a", dur = StepDuration.ticks(3))
      val b = step("b", dur = StepDuration.ticks(5))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(a.id, b.id)))
      k.update(Update.Reorder(List(b.id, a.id), None))
      k.get(b.id).now().start shouldBe Some(Duration.Zero)
      k.get(a.id).now().start shouldBe Some(ticks(5))
      k.endTime.now() shouldBe ticks(8)
    }

    "reordering untimed root before timed root: untimed has no start" in {
      val a = step("a")
      val b = step("b", dur = StepDuration.ticks(4))
      val k = keeper(forest(Map(a.id -> a, b.id -> b), roots = List(b.id, a.id)))
      // initially: b anchors at 0, a has no duration so start=Some(ticks(4))
      k.update(Update.Reorder(List(a.id, b.id), None))
      // a is now first but has no duration → start=None
      k.get(a.id).now().start shouldBe None
      // b is second and has duration → anchors at 0
      k.get(b.id).now().start shouldBe Some(Duration.Zero)
    }

    "reordering children updates their starts" in {
      val parent = step("p", dur = StepDuration.ticks(1))
      val c1     = step("c1", dur = StepDuration.ticks(2))
      val c2     = step("c2", dur = StepDuration.ticks(3))
      val k = keeper(forest(
        Map(parent.id -> parent, c1.id -> c1, c2.id -> c2),
        parentsToChildren = Map(parent.id -> List(c1.id, c2.id)),
        roots = List(parent.id)
      ))
      k.update(Update.Reorder(List(c2.id, c1.id), Some(parent.id)))
      // c2 is now first child: parent.start(0) + parent.own(1) = 1
      k.get(c2.id).now().start shouldBe Some(ticks(1))
      // c1 follows c2: 1 + 3 = 4
      k.get(c1.id).now().start shouldBe Some(ticks(4))
    }
  }
  
  "get" - {
    "returns updated state after update is applied" in {
      val a = step("a")
      val k = keeper(forest(Map(a.id -> a), roots = List(a.id)))
      val signal = k.get(a.id)
      signal.now().start shouldBe None
      k.update(Update.UpdateData(a.id, step("a", dur = StepDuration.ticks(4))))
      signal.now().start shouldBe Some(Duration.Zero)
    }

    "returns updated state after a step is added" in {
      val a = step("a", dur = StepDuration.ticks(4))
      val k = keeper(Forest.empty)
      val signal = k.get(a.id)
      signal.now().start shouldBe None
      k.update(Update.AddNode(a.id, a))
      signal.now().start shouldBe Some(Duration.Zero)
    }
  }
}
