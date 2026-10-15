package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Repetitions, StopCondition}
import com.leagueplans.ui.projection.calculation.loops.{LoopContext, Shortfalls, StopConditions}
import com.leagueplans.ui.projection.model.{LoopRun, StepTiming}
import com.leagueplans.uicommon.utils.scala.DurationOps.{safeAdd, safeMul}
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.Duration

private[calculation] object ForestFolder {
  type Iteration[ID, T] = (id: ID, value: T, reps: Int)

  /** The most repetitions a step that repeats until a condition is met can play */
  val loopCap: Int = 100000

  /** What the folder needs to play steps that repeat until a condition is met, and to time them */
  trait Looping[T, Acc] {
    def repetitions(value: T): Repetitions

    /** How long a value's own part takes, or None if it takes no time */
    def duration(value: T): Option[Duration]

    def context(acc: Acc, shortfalls: Shortfalls): LoopContext

    /** The items that playing a value's own part `reps` times from `acc` would try to take but
      * not find */
    def shortfalls(acc: Acc, value: T, reps: Int): Shortfalls
  }

  /** @param loops each pass of each step that repeats until a condition is met, in the order
    *              they were played
    * @param timings when each step happens and how long it takes, apart from whether it's inside a
    *                loop, which isn't worked out here
    * @param elapsed how far into the plan the fold ended
    */
  final case class Result[ID, Acc](
    acc: Acc,
    loops: Map[ID, Vector[LoopRun]],
    timings: Map[ID, StepTiming],
    elapsed: Option[Duration]
  )

  /** Folds over every repetition of every step, depth first.
    *
    * A step with a fixed number of repetitions plays its own part and then its substeps, that many
    * times. A step that repeats until a condition is met does the same, checking the condition
    * after each whole repetition. A repetition that would take a condition asking to stop before
    * it's passed past its threshold is played and then thrown away, along with anything found
    * while playing it.
    *
    * Steps are timed as they're played. A step that's never played, because it's inside one that
    * plays no repetitions, is timed as if it were played once.
    *
    * @param elapsed how far into the plan the fold starts
    * @param withholdLastRepOf a step whose last repetition is left out of the result, which is
    *                          then the fold as it was before that repetition. For a fixed number
    *                          of repetitions, give the step one fewer instead.
    */
  def foldLeftAsync[T, ID, Acc](
    forest: Forest[ID, T],
    initialAcc: Acc,
    signal: dom.AbortSignal,
    looping: Looping[T, Acc],
    elapsed: Option[Duration] = None,
    withholdLastRepOf: Option[ID] = None,
    yieldInterval: Int = 500
  )(f: (Acc, T, Int) => Acc): Future[Option[Result[ID, Acc]]] = {
    val runner = Runner(forest, looping, withholdLastRepOf, yieldInterval, f)
    val initialState = State[ID, Acc](initialAcc, Map.empty, List.empty, elapsed, List.empty, Map.empty)
    runner.run(initialState, forest.roots.flatMap(runner.framesFor), signal).map(_.map(state =>
      Result(state.acc, state.loops, state.timings, state.elapsed)
    ))
  }

  /** A fold over steps that only ever repeat a fixed number of times, and take no time */
  def foldLeftAsync[T, ID, Acc](
    forest: Forest[ID, Iteration[ID, T]],
    initialAcc: Acc,
    initialRemaining: List[Iteration[ID, T]],
    signal: dom.AbortSignal,
    yieldInterval: Int
  )(f: (Acc, T, Int) => Acc): Future[Option[Acc]] = {
    val looping = new Looping[Iteration[ID, T], Acc] {
      def repetitions(value: Iteration[ID, T]): Repetitions = Repetitions.Fixed(value.reps)
      def duration(value: Iteration[ID, T]): Option[Duration] = None
      def context(acc: Acc, shortfalls: Shortfalls): LoopContext =
        throw IllegalStateException("A fixed number of repetitions has no condition to check")
      def shortfalls(acc: Acc, value: Iteration[ID, T], reps: Int): Shortfalls = Shortfalls.none
    }
    val runner = Runner[Iteration[ID, T], ID, Acc](forest, looping, None, yieldInterval, (acc, it, n) => f(acc, it.value, n))
    val remaining = initialRemaining.flatMap(it => runner.framesFor(it.id, it, Repetitions.Fixed(it.reps)))
    val initialState = State[ID, Acc](initialAcc, Map.empty, List.empty, None, List.empty, Map.empty)
    runner.run(initialState, remaining, signal).map(_.map(_.acc))
  }

  /** A pass of a step that has started and not yet finished
    *
    * @param start when the pass started
    * @param taken how long the pass has taken so far
    * @param firstRep how long the pass's first repetition took, once it has finished
    */
  private final case class Open[ID](
    id: ID,
    start: Option[Duration],
    taken: Option[Duration],
    firstRep: Option[Option[Duration]]
  )

  /** @param tallies the items that each repetition in progress has tried to take but not found,
    *                innermost first, alongside whether its condition needs them
    * @param elapsed how far into the plan the fold is, outside of any open pass
    * @param open the passes in progress, innermost first
    */
  private final case class State[ID, Acc](
    acc: Acc,
    loops: Map[ID, Vector[LoopRun]],
    tallies: List[(Boolean, Shortfalls)],
    elapsed: Option[Duration],
    open: List[Open[ID]],
    timings: Map[ID, StepTiming]
  ) {
    def record(id: ID, run: LoopRun): State[ID, Acc] =
      copy(loops = loops.updated(id, loops.getOrElse(id, Vector.empty) :+ run))

    /** How far into the plan the fold is */
    def now: Option[Duration] =
      open.headOption.fold(elapsed)(pass => add(pass.start, pass.taken))
  }

  private def add(a: Option[Duration], b: Option[Duration]): Option[Duration] =
    (a, b) match {
      case (Some(x), Some(y)) => Some(x.safeAdd(y))
      case (x, None) => x
      case (None, y) => y
    }

  private enum Frame[ID, T, Acc] {
    /** Starts a pass of a step */
    case Enter(id: ID)
    /** Plays a step a fixed number more times, having played it `played` times in this pass */
    case Visit(id: ID, value: T, reps: Int, played: Int)
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
    /** Marks the end of the first repetition of the innermost open pass */
    case FirstRepDone()
    /** Finishes a pass of a step */
    case Exit(id: ID, value: T, repetitions: Repetitions)
  }

  private final class Runner[T, ID, Acc](
    forest: Forest[ID, T],
    looping: Looping[T, Acc],
    withholdLastRepOf: Option[ID],
    yieldInterval: Int,
    f: (Acc, T, Int) => Acc
  ) {
    def framesFor(id: ID): List[Frame[ID, T, Acc]] =
      forest.get(id).toList.flatMap(value => framesFor(id, value, looping.repetitions(value)))

    def framesFor(id: ID, value: T, repetitions: Repetitions): List[Frame[ID, T, Acc]] = {
      val main = repetitions match {
        case Repetitions.Fixed(count) => Frame.Visit[ID, T, Acc](id, value, count, played = 0)
        case Repetitions.Until(condition, max) => Frame.StartLoop[ID, T, Acc](id, value, condition, max)
      }
      List(Frame.Enter(id), main, Frame.Exit(id, value, repetitions))
    }

    private def childIDs(id: ID): List[ID] =
      forest.toChildren.getOrElse(id, List.empty).filter(forest.nodes.contains)

    // How long a step's repetition would take with every step played as many times as it says,
    // and a step that repeats until a condition is met played once
    private val perRepCache = mutable.Map.empty[ID, Option[Duration]]

    private def perRep(id: ID): Option[Duration] =
      perRepCache.getOrElseUpdate(id,
        childIDs(id).foldLeft(forest.get(id).flatMap(looping.duration))((acc, child) => add(acc, perPass(child)))
      )

    private def perPass(id: ID): Option[Duration] =
      forest.get(id).flatMap(value => perRep(id).map(_.safeMul(looping.repetitions(value).assumed)))

    /** Whether anything in the step takes time */
    private def timed(id: ID): Boolean =
      perRep(id).isDefined

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
          case Frame.Enter(id) =>
            state = enter(state, id)
            remaining = t

          case Frame.Visit(_, _, reps, _) if reps <= 0 =>
            remaining = t

          case Frame.Visit(id, value, reps, played) =>
            val children = childIDs(id)
            if (children.isEmpty) {
              val batch = math.min(reps, repsUntilYield)
              state = play(state, value, batch)
              if (played == 0) state = firstRepDone(state, looping.duration(value))
              remaining = if (batch < reps) Frame.Visit(id, value, reps - batch, played + batch) +: t else t
              repsUntilYield -= batch
            } else {
              state = play(state, value, 1)
              val marker = Option.when(played == 0)(Frame.FirstRepDone[ID, T, Acc]()).toList
              remaining = children.flatMap(framesFor) ++ marker ++ (Frame.Visit(id, value, reps - 1, played + 1) +: t)
              repsUntilYield -= 1
            }

          case Frame.StartLoop(id, value, condition, max) =>
            val limit = max.fold(loopCap)(math.min(_, loopCap))
            if (StopConditions.met(condition, contextOf(state, Shortfalls.none))) {
              state = state.record(id, LoopRun(0, LoopRun.Outcome.AlreadyMet, List.empty))
              remaining = t
            } else if (limit <= 0) {
              state = state.record(id, LoopRun(0, LoopRun.Outcome.ReachedMax(limit), List.empty))
              remaining = t
            } else {
              val check = Frame.CheckRep(id, value, condition, max, 0, state, Shortfalls.none, None, List.empty)
              val (started, body) = startRep(state, id, value, condition, first = true)
              state = started
              remaining = body ++ (check +: t)
              repsUntilYield -= 1
            }

          case check @ Frame.CheckRep(_, _, _, _, _, _, _, _, _) =>
            val (updated, next) = decide(state, check)
            state = updated
            remaining = next ++ t
            repsUntilYield -= 1

          case Frame.FirstRepDone() =>
            state = firstRepDone(state, state.open.head.taken)
            remaining = t

          case Frame.Exit(id, value, repetitions) =>
            state = exit(state, id, value, repetitions)
            remaining = t
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
      val open = state.open match {
        case pass :: outer => pass.copy(taken = add(pass.taken, looping.duration(value).map(_.safeMul(reps)))) :: outer
        case Nil => Nil
      }
      state.copy(acc = f(state.acc, value, reps), tallies = tallies, open = open)
    }

    /** Plays the step's own part for a new repetition, returning the frames for the rest of it */
    private def startRep(
      state: State[ID, Acc],
      id: ID,
      value: T,
      condition: StopCondition,
      first: Boolean
    ): (State[ID, Acc], List[Frame[ID, T, Acc]]) = {
      val needsShortfalls = StopConditions.leaves(condition).exists(_.isInstanceOf[StopCondition.ItemCount])
      val withTally = state.copy(tallies = (needsShortfalls, Shortfalls.none) :: state.tallies)
      val marker = Option.when(first)(Frame.FirstRepDone[ID, T, Acc]()).toList
      (play(withTally, value, 1), childIDs(id).flatMap(framesFor) ++ marker)
    }

    private def decide(
      state: State[ID, Acc],
      check: Frame.CheckRep[ID, T, Acc]
    ): (State[ID, Acc], List[Frame[ID, T, Acc]]) = {
      import check.*
      val (tally, outerTallies) = (state.tallies.head._2, state.tallies.tail)
      val beforeContext = contextOf(before, beforeShortfalls)
      val afterContext = contextOf(state, tally)
      val passing = StopConditions.passed(condition, beforeContext, afterContext)

      if (StopConditions.metBefore(condition, beforeContext, afterContext)) {
        // The repetition goes past a condition that asks to stop before it does, and so is thrown away
        val run = LoopRun(completed, LoopRun.Outcome.StoppedBefore(passing), passedOn)
        val kept = if (withholdLastRepOf.contains(id)) lastBefore.getOrElse(before) else before
        (kept.record(id, run), List.empty)
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
            val kept = if (withholdLastRepOf.contains(id)) before else accepted
            (kept.record(id, run), List.empty)

          case None =>
            val nextCheck = Frame.CheckRep(id, value, condition, max, reps, accepted, tally, Some(before), updatedPassedOn)
            val (started, body) = startRep(accepted, id, value, condition, first = false)
            (started, body :+ nextCheck)
        }
      }
    }

    /** For the step that repeats, whose pass is the innermost one open */
    private def contextOf(state: State[ID, Acc], shortfalls: Shortfalls): LoopContext = {
      val now = state.now.getOrElse(Duration.Zero)
      val stepStart = state.open.headOption.flatMap(_.start).getOrElse(Duration.Zero)
      looping.context(state.acc, shortfalls).copy(inStep = now - stepStart, inPlan = now)
    }

    private def enter(state: State[ID, Acc], id: ID): State[ID, Acc] = {
      val start = state.now.orElse(Option.when(timed(id))(Duration.Zero))
      state.copy(open = Open(id, start, taken = None, firstRep = None) :: state.open)
    }

    private def firstRepDone(state: State[ID, Acc], taken: Option[Duration]): State[ID, Acc] =
      state.open match {
        case pass :: outer if pass.firstRep.isEmpty => state.copy(open = pass.copy(firstRep = Some(taken)) :: outer)
        case _ => state
      }

    private def exit(state: State[ID, Acc], id: ID, value: T, repetitions: Repetitions): State[ID, Acc] = {
      val (pass, outer) = (state.open.head, state.open.tail)
      val reps = repetitions match {
        case Repetitions.Fixed(count) => math.max(count, 0)
        case Repetitions.Until(_, _) => state.loops.get(id).flatMap(_.lastOption).fold(0)(_.reps)
      }
      val duration = Option.when(timed(id))(pass.taken.getOrElse(Duration.Zero))
      val finish = add(pass.start, duration)

      val timing = state.timings.get(id) match {
        case None =>
          val firstRep = pass.firstRep.filter(_ => reps > 0).map(taken =>
            Option.when(timed(id))(taken.getOrElse(Duration.Zero))
          )
          StepTiming(pass.start, firstRep.getOrElse(perRep(id)), duration, duration, reps, insideLoop = false)
        case Some(existing) =>
          existing.copy(total = add(existing.total, duration), totalReps = existing.totalReps + reps)
      }
      val timings = state.timings.updated(id, timing)

      // The substeps of a pass with no repetitions aren't played, but are still timed for one
      val withUnplayed =
        if (reps == 0) timeUnplayed(childIDs(id), add(pass.start, looping.duration(value)), timings)
        else timings

      outer match {
        case parent :: rest =>
          state.copy(open = parent.copy(taken = add(parent.taken, duration)) :: rest, timings = withUnplayed)
        case Nil =>
          state.copy(open = Nil, elapsed = finish, timings = withUnplayed)
      }
    }

    private def timeUnplayed(
      ids: List[ID],
      running: Option[Duration],
      timings: Map[ID, StepTiming]
    ): Map[ID, StepTiming] =
      ids.foldLeft((running, timings)) { case ((running, timings), id) =>
        val start = running.orElse(Option.when(timed(id))(Duration.Zero))
        val firstPass = perPass(id)
        val withStep =
          if (timings.contains(id)) timings
          else timings.updated(id, StepTiming(start, perRep(id), firstPass, firstPass.map(_ => Duration.Zero), 0, insideLoop = false))
        val childrenStart = add(start, forest.get(id).flatMap(looping.duration))
        (add(start, firstPass), timeUnplayed(childIDs(id), childrenStart, withStep))
      }._2
  }
}
