package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Repetitions, StopCondition}
import com.leagueplans.ui.projection.calculation.loops.{LoopContext, Shortfalls, StopConditions}
import com.leagueplans.ui.projection.model.LoopRun
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

private[calculation] object ForestFolder {
  type Iteration[ID, T] = (id: ID, value: T, reps: Int)

  /** The most repetitions a step that repeats until a condition is met can play */
  val loopCap: Int = 100000

  /** What the folder needs to play steps that repeat until a condition is met */
  trait Looping[T, Acc] {
    def repetitions(value: T): Repetitions

    def context(acc: Acc, shortfalls: Shortfalls): LoopContext

    /** The items that playing a value's own part `reps` times from `acc` would try to take but
      * not find */
    def shortfalls(acc: Acc, value: T, reps: Int): Shortfalls
  }

  /** @param loops each pass of each step that repeats until a condition is met, in the order
    *              they were played
    */
  final case class Result[ID, Acc](acc: Acc, loops: Map[ID, Vector[LoopRun]])

  /** Folds over every repetition of every step, depth first.
    *
    * A step with a fixed number of repetitions plays its own part and then its substeps, that many
    * times. A step that repeats until a condition is met does the same, checking the condition
    * after each whole repetition. A repetition that would take a condition asking to stop before
    * it's passed past its threshold is played and then thrown away, along with anything found
    * while playing it.
    *
    * @param withholdLastRepOf a step whose last repetition is left out of the result, which is
    *                          then the accumulator from before it. For a fixed number of
    *                          repetitions, give the step one fewer instead.
    */
  def foldLeftAsync[T, ID, Acc](
    forest: Forest[ID, T],
    initialAcc: Acc,
    signal: dom.AbortSignal,
    looping: Looping[T, Acc],
    withholdLastRepOf: Option[ID] = None,
    yieldInterval: Int = 500
  )(f: (Acc, T, Int) => Acc): Future[Option[Result[ID, Acc]]] = {
    val runner = Runner(forest, looping, withholdLastRepOf, yieldInterval, f)
    val initialState = State[ID, Acc](initialAcc, Map.empty, List.empty)
    runner.run(initialState, forest.roots.flatMap(runner.frameFor), signal).map(_.map(state =>
      Result(state.acc, state.loops)
    ))
  }

  /** A fold over steps that only ever repeat a fixed number of times */
  def foldLeftAsync[T, ID, Acc](
    forest: Forest[ID, Iteration[ID, T]],
    initialAcc: Acc,
    initialRemaining: List[Iteration[ID, T]],
    signal: dom.AbortSignal,
    yieldInterval: Int
  )(f: (Acc, T, Int) => Acc): Future[Option[Acc]] = {
    val looping = new Looping[Iteration[ID, T], Acc] {
      def repetitions(value: Iteration[ID, T]): Repetitions = Repetitions.Fixed(value.reps)
      def context(acc: Acc, shortfalls: Shortfalls): LoopContext =
        throw IllegalStateException("A fixed number of repetitions has no condition to check")
      def shortfalls(acc: Acc, value: Iteration[ID, T], reps: Int): Shortfalls = Shortfalls.none
    }
    val runner = Runner[Iteration[ID, T], ID, Acc](forest, looping, None, yieldInterval, (acc, it, n) => f(acc, it.value, n))
    val remaining = initialRemaining.map(it => Frame.Visit[ID, Iteration[ID, T], Acc](it.id, it, it.reps))
    runner.run(State[ID, Acc](initialAcc, Map.empty, List.empty), remaining, signal).map(_.map(_.acc))
  }

  /** @param tallies the items that each repetition in progress has tried to take but not found,
    *                innermost first, alongside whether its condition needs them
    */
  private final case class State[ID, Acc](
    acc: Acc,
    loops: Map[ID, Vector[LoopRun]],
    tallies: List[(Boolean, Shortfalls)]
  ) {
    def record(id: ID, run: LoopRun): State[ID, Acc] =
      copy(loops = loops.updated(id, loops.getOrElse(id, Vector.empty) :+ run))
  }

  private enum Frame[ID, T, Acc] {
    /** Plays a step a fixed number more times */
    case Visit(id: ID, value: T, reps: Int)
    /** Starts a step that repeats until a condition is met */
    case StartLoop(id: ID, value: T, condition: StopCondition, max: Option[Int])
    /** Decides whether a step that repeats until a condition is met plays again, once the
      * repetition after `completed` repetitions has been played
      *
      * @param before the state before that repetition
      * @param beforeShortfalls what the repetition before that one tried to take but didn't find
      * @param lastBefore the state before the last repetition, if one's been kept
      */
    case CheckRep(
      id: ID,
      value: T,
      condition: StopCondition,
      max: Option[Int],
      completed: Int,
      before: State[ID, Acc],
      beforeShortfalls: Shortfalls,
      lastBefore: Option[State[ID, Acc]],
      passedOn: List[LoopRun.Passed]
    )
  }

  private final class Runner[T, ID, Acc](
    forest: Forest[ID, T],
    looping: Looping[T, Acc],
    withholdLastRepOf: Option[ID],
    yieldInterval: Int,
    f: (Acc, T, Int) => Acc
  ) {
    def frameFor(id: ID): Option[Frame[ID, T, Acc]] =
      forest.get(id).map(value =>
        looping.repetitions(value) match {
          case Repetitions.Fixed(count) => Frame.Visit(id, value, count)
          case Repetitions.Until(condition, max) => Frame.StartLoop(id, value, condition, max)
        }
      )

    def run(
      initialState: State[ID, Acc],
      initialRemaining: List[Frame[ID, T, Acc]],
      signal: dom.AbortSignal
    ): Future[Option[State[ID, Acc]]] = {
      var state = initialState
      var remaining = initialRemaining
      var repsUntilYield = yieldInterval

      while (remaining.nonEmpty && repsUntilYield > 0 && !signal.aborted) {
        val (h, t) = (remaining.head, remaining.tail)
        h match {
          case Frame.Visit(_, _, reps) if reps <= 0 =>
            remaining = t

          case Frame.Visit(id, value, reps) =>
            val children = forest.toChildren.getOrElse(id, List.empty)
            if (children.isEmpty) {
              val batch = math.min(reps, repsUntilYield)
              state = play(state, value, batch)
              remaining = if (batch < reps) Frame.Visit(id, value, reps - batch) +: t else t
              repsUntilYield -= batch
            } else {
              state = play(state, value, 1)
              remaining = (children.flatMap(frameFor) :+ Frame.Visit(id, value, reps - 1)) ++ t
              repsUntilYield -= 1
            }

          case Frame.StartLoop(id, value, condition, max) =>
            val limit = max.fold(loopCap)(math.min(_, loopCap))
            if (StopConditions.met(condition, looping.context(state.acc, Shortfalls.none))) {
              state = state.record(id, LoopRun(0, LoopRun.Outcome.AlreadyMet, List.empty))
              remaining = t
            } else if (limit <= 0) {
              state = state.record(id, LoopRun(0, LoopRun.Outcome.ReachedMax(limit), List.empty))
              remaining = t
            } else {
              val check = Frame.CheckRep(id, value, condition, max, 0, state, Shortfalls.none, None, List.empty)
              val (started, body) = startRep(state, id, value, condition)
              state = started
              remaining = body ++ (check +: t)
              repsUntilYield -= 1
            }

          case check @ Frame.CheckRep(_, _, _, _, _, _, _, _, _) =>
            val (updated, next) = decide(state, check)
            state = updated
            remaining = next ++ t
            repsUntilYield -= 1
        }
      }

      if (signal.aborted) Future.successful(None)
      else if (remaining.isEmpty) Future.successful(Some(state))
      else Future.unit.flatMap(_ => run(state, remaining, signal))
    }

    /** Plays a step's own part, adding anything it can't find to the repetition in progress */
    private def play(state: State[ID, Acc], value: T, reps: Int): State[ID, Acc] = {
      val tallies = state.tallies match {
        case (needed, tally) :: outer if (needed :: outer.map(_._1)).contains(true) =>
          (needed, Shortfalls.combine(tally, looping.shortfalls(state.acc, value, reps))) :: outer
        case other =>
          other
      }
      state.copy(acc = f(state.acc, value, reps), tallies = tallies)
    }

    /** Plays the step's own part for a new repetition, returning the frames for its substeps */
    private def startRep(
      state: State[ID, Acc],
      id: ID,
      value: T,
      condition: StopCondition
    ): (State[ID, Acc], List[Frame[ID, T, Acc]]) = {
      val needsShortfalls = StopConditions.leaves(condition).exists(_.isInstanceOf[StopCondition.ItemCount])
      val withTally = state.copy(tallies = (needsShortfalls, Shortfalls.none) :: state.tallies)
      (play(withTally, value, 1), forest.toChildren.getOrElse(id, List.empty).flatMap(frameFor))
    }

    private def decide(
      state: State[ID, Acc],
      check: Frame.CheckRep[ID, T, Acc]
    ): (State[ID, Acc], List[Frame[ID, T, Acc]]) = {
      import check.*
      val (tally, outerTallies) = (state.tallies.head._2, state.tallies.tail)
      val beforeContext = looping.context(before.acc, beforeShortfalls)
      val afterContext = looping.context(state.acc, tally)
      val passing = StopConditions.passed(condition, beforeContext, afterContext)

      if (StopConditions.metBefore(condition, beforeContext, afterContext)) {
        // The repetition goes past a condition that asks to stop before it does, and so is thrown away
        val run = LoopRun(completed, LoopRun.Outcome.StoppedBefore(passing), passedOn)
        val kept = if (withholdLastRepOf.contains(id)) lastBefore.getOrElse(before) else before
        (before.copy(acc = kept.acc).record(id, run), List.empty)
      } else {
        val reps = completed + 1
        val updatedPassedOn = if (passing.isEmpty) passedOn else passedOn :+ LoopRun.Passed(reps, passing)
        val accepted = state.copy(tallies = outerTallies match {
          case (needed, outer) :: rest => (needed, Shortfalls.combine(outer, tally)) :: rest
          case Nil => Nil
        })

        lazy val estimate = StopConditions.estimate(condition, beforeContext, afterContext)
        val maybeOutcome =
          if (StopConditions.met(condition, afterContext)) Some(LoopRun.Outcome.Met)
          else if (max.exists(reps >= _)) max.map(LoopRun.Outcome.ReachedMax(_))
          else if (reps >= loopCap) Some(LoopRun.Outcome.ReachedCap(loopCap))
          else if (!StopConditions.progressed(condition, beforeContext, afterContext)) Some(LoopRun.Outcome.NoProgress)
          // A step with a lower limit of its own stops there, however slow it is
          else if (max.exists(_ < loopCap)) None
          else estimate.filter(reps + _ > loopCap).map(e => LoopRun.Outcome.TooSlow(reps + e))

        maybeOutcome match {
          case Some(outcome) =>
            val run = LoopRun(reps, outcome, updatedPassedOn)
            val kept = if (withholdLastRepOf.contains(id)) before.acc else accepted.acc
            (accepted.copy(acc = kept).record(id, run), List.empty)

          case None =>
            val nextCheck = Frame.CheckRep(id, value, condition, max, reps, accepted, tally, Some(before), updatedPassedOn)
            val (started, body) = startRep(accepted, id, value, condition)
            (started, body :+ nextCheck)
        }
      }
    }
  }
}
