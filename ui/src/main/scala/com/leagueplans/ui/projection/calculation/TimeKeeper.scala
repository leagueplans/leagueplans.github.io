package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.{Forest, ForestResolver}
import com.leagueplans.ui.model.plan.{Step, Duration as StepDuration}
import com.leagueplans.ui.projection.calculation.TimeKeeper.{State, addDurations}
import com.leagueplans.ui.utils.scala.DurationOps.{safeAdd, safeMul}
import com.raquo.airstream.state.{StrictSignal, Var}

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.duration.Duration

object TimeKeeper {
  final case class State(
    start: Option[Duration],
    stepDuration: Option[StepDuration],
    totalChildDuration: Option[Duration],
    repetitions: Int
  ) {
    val durationPerRep: Option[Duration] =
      addDurations(stepDuration.map(_.asScala), totalChildDuration)

    val durationPerParentRep: Option[Duration] =
      durationPerRep.map(_.safeMul(repetitions))

    val childrenStart: Option[Duration] =
      addDurations(start, stepDuration.map(_.asScala))

    val finish: Option[Duration] =
      addDurations(start, durationPerParentRep)
  }

  object State {
    val zero: State = State(
      start = None,
      stepDuration = None,
      totalChildDuration = None,
      repetitions = 1
    )
  }

  def apply(forest: Forest[Step.ID, Step]): TimeKeeper = {
    val stateForest = forest.map((_, step) =>
      State(start = None, step.duration.toOption, totalChildDuration = None, step.repetitions)
    )
    val keeper = new TimeKeeper(stateForest)
    keeper.recomputeAll()
    keeper
  }

  private def addDurations(a: Option[Duration], b: Option[Duration]): Option[Duration] =
    (a, b) match {
      case (Some(x), Some(y)) => Some(x.safeAdd(y))
      case (Some(x), None) => Some(x)
      case (None, Some(y)) => Some(y)
      case (None, None) => None
    }
}

final class TimeKeeper(private var forest: Forest[Step.ID, State]) {
  private val _endTime: Var[Duration] = Var(Duration.Zero)

  private val stateVars: mutable.Map[Step.ID, Var[State]] =
    mutable.Map.from(forest.nodes.view.map((id, state) => id -> Var(state)))

  /** Holds Vars pre-created by [[get]] for steps not yet in the forest. When the corresponding
   *  [[Forest.Update.AddNode]] arrives, [[update]] retrieves the Var from here rather than
   *  creating a new one, so any signals already derived from it stay live. */
  private val pendingVars: mutable.Map[Step.ID, Var[State]] = mutable.Map.empty

  val endTime: StrictSignal[Duration] =
    _endTime.signal

  def get(step: Step.ID): StrictSignal[State] =
    stateVars.get(step) match {
      case Some(v) => v.signal
      case None    => pendingVars.getOrElseUpdate(step, Var(State.zero)).signal
    }

  private def setState(id: Step.ID, state: State): Unit = {
    forest = ForestResolver.resolve(forest, Update.UpdateData(id, state))
    stateVars.get(id).foreach(_.set(state))
  }

  def update(forestUpdate: Forest.Update[Step.ID, Step]): Unit =
    forestUpdate match {
      case Update.AddNode(id, data) =>
        // New root appended at end; compute its start from the previous last root's finish
        val runningStart = for {
          root <- forest.roots.lastOption
          state <- forest.get(root)
          finish <- state.finish
        } yield finish
        val durationPerParentRep = data.duration.toOption.map(_.asScala.safeMul(data.repetitions))
        val effectiveStart = computeEffectiveStart(runningStart, durationPerParentRep)
        val initialState = State(effectiveStart, data.duration.toOption, totalChildDuration = None, data.repetitions)
        val v = pendingVars.remove(id).getOrElse(stateVars.getOrElse(id, Var(initialState)))
        stateVars.put(id, v)
        v.set(initialState)
        forest = ForestResolver.resolve(forest, Update.AddNode(id, initialState))
        updateEndTime()

      case Update.RemoveNode(id) =>
        val idStart = forest.get(id).flatMap(_.start)
        val following = forest.siblings(id).dropWhile(_ != id).drop(1)
        val maybeParent = forest.toParent.get(id)
        forest = ForestResolver.resolve(forest, Update.RemoveNode(id))
        stateVars.remove(id)
        recomputeSequence(following, idStart)
        maybeParent.foreach { p =>
          if (recomputeTotalChildDuration(p)) propagate(p)
        }
        updateEndTime()

      case Update.AddLink(child, parent) =>
        val childOldStart = forest.get(child).flatMap(_.start)
        // AddLink only fires when child is a root (ChangeParent handles non-root moves via
        // RemoveLink + AddLink), so child's current siblings are the other roots.
        val oldFollowingRoots = forest.siblings(child).dropWhile(_ != child).drop(1)
        forest = ForestResolver.resolve(forest, Update.AddLink(child, parent))
        // 1. Heal the gap left in roots
        recomputeSequence(oldFollowingRoots, childOldStart)
        // 2. Recompute child's start at its new position (last child of parent)
        recomputeSubtreeStarts(child, runningStartFor(child))
        // 3. Update parent's timing
        if (recomputeTotalChildDuration(parent)) propagate(parent)
        updateEndTime()

      case Update.RemoveLink(child, parent) =>
        val childOldStart = forest.get(child).flatMap(_.start)
        val oldFollowing = forest.siblings(child).dropWhile(_ != child).drop(1)
        forest = ForestResolver.resolve(forest, Update.RemoveLink(child, parent))
        // 1. Heal gap among former siblings
        recomputeSequence(oldFollowing, childOldStart)
        // 2. Recompute timings from the parent onwards
        if (recomputeTotalChildDuration(parent)) propagate(parent)
        else {
          val newRootStart = for {
            root <- forest.roots.dropRight(1).lastOption
            state <- forest.get(root)
            finish <- state.finish
          } yield finish
          recomputeSubtreeStarts(child, newRootStart)
        }
        updateEndTime()

      case Update.ChangeParent(child, oldParent, newParent) =>
        update(Update.RemoveLink(child, oldParent))
        update(Update.AddLink(child, newParent))

      case Update.UpdateData(id, data) =>
        forest.get(id).foreach { old =>
          val newDuration = data.duration.toOption
          if (old.stepDuration != newDuration || old.repetitions != data.repetitions) {
            setState(id, old.copy(stepDuration = newDuration, repetitions = data.repetitions))
            recomputeSubtreeStarts(id, runningStartFor(id))
            propagate(id)
          }
        }
        updateEndTime()

      case Update.Reorder(children, maybeParent) =>
        forest = ForestResolver.resolve(forest, Update.Reorder(children, maybeParent))
        val firstStart = for {
          p <- maybeParent
          state <- forest.get(p)
          childrenStart <- state.childrenStart
        } yield childrenStart
        recomputeSequence(children, firstStart)
    }

  private def computeEffectiveStart(
    runningStart: Option[Duration],
    totalDuration: Option[Duration]
  ): Option[Duration] =
    runningStart.orElse(
      Option.when(totalDuration.isDefined)(Duration.Zero)
    )

  private def recomputeTotalChildDuration(id: Step.ID): Boolean =
    forest.get(id) match {
      case None => false
      case Some(old) =>
        val children = forest.toChildren.getOrElse(id, Nil)
        val newTotal = children.foldLeft(Option.empty[Duration]) { (acc, child) =>
          addDurations(acc, forest.get(child).flatMap(_.durationPerParentRep))
        }
        val totalChanged = newTotal != old.totalChildDuration
        if (totalChanged) setState(id, old.copy(totalChildDuration = newTotal))
        totalChanged
    }

  private def recomputeSubtreeStarts(id: Step.ID, runningStart: Option[Duration]): Unit =
    forest.get(id).foreach { current =>
      val effectiveStart = computeEffectiveStart(runningStart, current.durationPerParentRep)
      if (effectiveStart != current.start) setState(id, current.copy(start = effectiveStart))
      recomputeSequence(
        forest.toChildren.getOrElse(id, Nil),
        addDurations(effectiveStart, current.stepDuration.map(_.asScala))
      )
    }

  @tailrec
  private def recomputeSequence(ids: List[Step.ID], start: Option[Duration]): Unit =
    ids match {
      case Nil => ()
      case h :: t =>
        recomputeSubtreeStarts(h, start)
        recomputeSequence(t, forest.get(h).flatMap(_.finish))
    }

  /** Returns the runningStart that should be passed to recomputeSubtreeStarts for `id`:
   *  the finish of the preceding sibling, or the parent start + stepDuration for a first child,
   *  or None for the first root. */
  private def runningStartFor(id: Step.ID): Option[Duration] =
    forest.siblings(id).takeWhile(_ != id).lastOption match {
      case Some(prevSib) => forest.get(prevSib).flatMap(_.finish)
      case None =>
        for {
          p <- forest.toParent.get(id)
          state <- forest.get(p)
          childrenStart <- state.childrenStart
        } yield childrenStart
    }

  @tailrec
  private def propagate(id: Step.ID): Unit = {
    val following = forest.siblings(id).dropWhile(_ != id).drop(1)
    recomputeSequence(following, forest.get(id).flatMap(_.finish))
    forest.toParent.get(id) match {
      case Some(parent) if recomputeTotalChildDuration(parent) => propagate(parent)
      case _ => ()
    }
  }

  private def updateEndTime(): Unit =
    _endTime.set(
      forest.roots.lastOption
        .flatMap(r => forest.get(r).flatMap(_.finish))
        .getOrElse(Duration.Zero)
    )

  private def recomputeAll(): Unit = {
    // Pass 1 (bottom-up): compute totalChildDuration for each node
    forest.toList.reverse.foreach(recomputeTotalChildDuration)
    // Pass 2 (top-down): compute starts for each root subtree
    recomputeSequence(forest.roots, None)
    updateEndTime()
  }
}
