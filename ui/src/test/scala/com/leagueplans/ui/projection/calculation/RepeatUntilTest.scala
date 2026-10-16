package com.leagueplans.ui.projection.calculation

import cats.data.NonEmptyList
import com.leagueplans.common.model.{InfoboxKey, Item, Skill}
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.*
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.mode.MainGame
import com.leagueplans.ui.model.player.skill.{Exp, Level, Stats}
import com.leagueplans.ui.model.player.{Cache, GridStatus, Player}
import com.leagueplans.ui.projection.calculation.loops.LoopMessages
import com.leagueplans.ui.projection.model.{LoopRun, Projection, StepError}
import org.scalajs.dom.AbortController
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.LoneElement
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.{Duration as ScalaDuration, DurationInt}
import scala.concurrent.{ExecutionContext, Future}

final class RepeatUntilTest extends AsyncFreeSpec with Matchers with LoneElement {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global

  private def item(id: Int, name: String, stackable: Boolean): Item =
    Item(
      Item.ID(id),
      gameID = None,
      name,
      examine = "",
      NonEmptyList.one((Item.Image.Bin(1), Item.Image.Path("1/1.png"))),
      Item.Bankable.Yes(stacks = true),
      stackable = stackable,
      noteable = false,
      equipmentType = None,
      infobox = InfoboxKey(id, List.empty)
    )

  private val feathers = item(314, "Feather", stackable = true)
  private val cache = Cache(Set(feathers), Set.empty, Set.empty, Set.empty, Set.empty)
  private val resolver = EffectResolver(List.empty, _ => 0, cache)
  private val player = Player(Stats(), Map.empty, Set.empty, Set.empty, LeagueStatus(0, Set.empty, Skill.values.toSet), GridStatus(Set.empty))

  private def step(id: String, repetitions: Repetitions, effects: Effect*): Step =
    Step(Step.ID.fromString(id), StepDetails(id).copy(directEffects = EffectList(effects.toList), repetitions = repetitions))

  private def fishing(xp: Int): Effect = Effect.GainExp(Skill.Fishing, 1, Exp(xp))
  private def addFeathers(n: Int): Effect = Effect.AddItem(feathers.id, ItemChange.By(n), Depository.Kind.Inventory, note = false)

  private def fishingLevel(level: Int, stopBefore: Boolean = false): StopCondition =
    StopCondition.SkillReaches(Skill.Fishing, ExpTarget.AtLevel(Level(level)), stopBefore)

  private def feathersAtMost(n: Int, stopBefore: Boolean): StopCondition =
    StopCondition.ItemCount(feathers.id, ItemLocation.Inventory, ItemForms.Unnoted, Comparison.AtMost, n, stopBefore)

  private def until(condition: StopCondition, max: Option[Int] = None): Repetitions =
    Repetitions.Until(condition, max)

  /** Steps given as (step, parent) in order */
  private def forest(steps: (Step, Option[Step])*): Forest[Step.ID, Step] =
    Forest.from(
      steps.map((s, _) => s.id -> s).toMap,
      steps.collect { case (s, Some(parent)) => parent.id -> s.id }.groupMap(_._1)(_._2).view.mapValues(_.toList).toMap,
      steps.collect { case (s, None) => s.id }.toList
    )

  private def find(forest: Forest[Step.ID, Step]): Future[StepErrorFinder.Findings] =
    new StepErrorFinder(Plan.Settings.Deferred(MainGame), player, resolver, cache)
      .findAsync(forest, new AbortController().signal)
      .map(_.get)

  private def project(forest: Forest[Step.ID, Step], focus: Step): Future[Projection] =
    new Projector(player, resolver, cache)
      .computeAsync(forest, Some(focus.id), new AbortController().signal)
      .map(_.get)

  private def fishingXp(player: Player): Int = player.stats(Skill.Fishing).raw / 10
  private def featherCount(player: Player): Int = player.get(Depository.Kind.Inventory).count(feathers.id, noted = false)

  "Repeat until" - {
    "plays a step until its condition is met, and no more" in {
      // Level 2 is 83 xp, which the second rep of 50 xp passes
      val loop = step("loop", until(fishingLevel(2)), fishing(50))
      val after = step("after", Repetitions.once)
      val f = forest(loop -> None, after -> None)
      for {
        findings <- find(f)
        projection <- project(f, after)
      } yield {
        findings.loops shouldBe Map(loop.id -> List(LoopRun(2, LoopRun.Outcome.Met(List(fishingLevel(2))), List.empty)))
        findings.errors shouldBe Map.empty
        fishingXp(projection.playerBeforeStep) shouldBe 100
      }
    }

    "doesn't play a step whose condition is already met, and says so" in {
      val first = step("first", Repetitions.once, fishing(100))
      val loop = step("loop", until(fishingLevel(2)), fishing(50))
      val after = step("after", Repetitions.once)
      val f = forest(first -> None, loop -> None, after -> None)
      for {
        findings <- find(f)
        projection <- project(f, after)
      } yield {
        findings.loops(loop.id) shouldBe List(LoopRun(0, LoopRun.Outcome.AlreadyMet(List(fishingLevel(2))), List.empty))
        findings.errors(loop.id).map(_.message) shouldBe
          List("Fishing is already level 2, so this step doesn't run")
        fishingXp(projection.playerBeforeStep) shouldBe 100
      }
    }

    "plays a step that brings its condition no closer once" in {
      val loop = step("loop", until(fishingLevel(2)), Effect.GainExp(Skill.Cooking, 1, Exp(50)))
      find(forest(loop -> None)).map { findings =>
        findings.loops(loop.id) shouldBe List(LoopRun(1, LoopRun.Outcome.NoProgress, List.empty))
        findings.errors(loop.id).map(_.message).loneElement shouldBe "Running this step again doesn't bring Fishing closer to level 2"
      }
    }

    "plays a step once when its condition would take more repetitions than any step can play" in {
      val loop = step("loop", until(fishingLevel(99)), fishing(1))
      find(forest(loop -> None)).map { findings =>
        findings.loops(loop.id).loneElement should matchPattern {
          case LoopRun(1, LoopRun.Outcome.TooSlow(_), Nil, Nil) =>
        }
      }
    }

    "plays a slow step up to its own limit" in {
      val loop = step("loop", until(fishingLevel(99), max = Some(5)), fishing(1))
      find(forest(loop -> None)).map { findings =>
        findings.loops(loop.id) shouldBe List(LoopRun(5, LoopRun.Outcome.ReachedMax(5), List.empty))
        findings.errors shouldBe Map.empty
      }
    }

    "counts items a repetition couldn't find as taking the count below zero" in {
      // 10 feathers, 3 a rep: the fourth rep wants 3 but finds 1
      val stock = step("stock", Repetitions.once, addFeathers(10))
      val loop = step("loop", until(feathersAtMost(0, stopBefore = false)), addFeathers(-3))
      find(forest(stock -> None, loop -> None)).map(
        _.loops(loop.id) shouldBe List(LoopRun(4, LoopRun.Outcome.Met(List(feathersAtMost(0, stopBefore = false))), List.empty))
      )
    }

    "stops before a repetition that would pass a condition asking it to" in {
      val stock = step("stock", Repetitions.once, addFeathers(10))
      val loop = step("loop", until(feathersAtMost(0, stopBefore = true)), addFeathers(-3), fishing(10))
      val after = step("after", Repetitions.once)
      val f = forest(stock -> None, loop -> None, after -> None)
      for {
        findings <- find(f)
        projection <- project(f, after)
      } yield {
        findings.loops(loop.id) shouldBe
          List(LoopRun(3, LoopRun.Outcome.StoppedBefore(List(feathersAtMost(0, stopBefore = true))), List.empty))
        // The fourth rep, and the problem it would have had, are thrown away
        findings.errors shouldBe Map.empty
        featherCount(projection.playerBeforeStep) shouldBe 1
        fishingXp(projection.playerBeforeStep) shouldBe 30
      }
    }

    "carries on past a condition asking to stop before it when the others aren't met, and says so" in {
      // 5 feathers, 3 a rep. Level 3 is 174 xp, met on the fourth rep of 50 xp. The second rep
      // takes the feathers below zero.
      val stock = step("stock", Repetitions.once, addFeathers(5))
      val condition = StopCondition.And(feathersAtMost(0, stopBefore = true), fishingLevel(3))
      val loop = step("loop", until(condition), addFeathers(-3), fishing(50))
      find(forest(stock -> None, loop -> None)).map { findings =>
        findings.loops(loop.id) shouldBe
          List(LoopRun(4, LoopRun.Outcome.Met(List(feathersAtMost(0, stopBefore = true), fishingLevel(3))), List(LoopRun.Passed(2, List(feathersAtMost(0, stopBefore = true))))))
        findings.errors(loop.id).map(_.message) should contain(
          "Feather runs out the 2nd time this step runs"
        )
      }
    }

    "repeats a step with substeps as a whole" in {
      val loop = step("loop", until(fishingLevel(2)))
      val child = step("child", Repetitions.Fixed(2), fishing(20))
      val after = step("after", Repetitions.once)
      val f = forest(loop -> None, child -> Some(loop), after -> None)
      for {
        findings <- find(f)
        projection <- project(f, after)
      } yield {
        // 40 xp a rep
        findings.loops(loop.id) shouldBe List(LoopRun(3, LoopRun.Outcome.Met(List(fishingLevel(2))), List.empty))
        fishingXp(projection.playerBeforeStep) shouldBe 120
      }
    }

    "counts each pass of a step inside another repeated step" in {
      val trip = step("trip", Repetitions.Fixed(3))
      val loop = step("loop", until(fishingLevel(2)), fishing(50))
      val f = forest(trip -> None, loop -> Some(trip))
      find(f).map { findings =>
        findings.loops(loop.id) shouldBe List(
          LoopRun(2, LoopRun.Outcome.Met(List(fishingLevel(2))), List.empty, List(LoopRun.Place("trip", 1))),
          LoopRun(0, LoopRun.Outcome.AlreadyMet(List(fishingLevel(2))), List.empty, List(LoopRun.Place("trip", 2))),
          LoopRun(0, LoopRun.Outcome.AlreadyMet(List(fishingLevel(2))), List.empty, List(LoopRun.Place("trip", 3)))
        )
        // Not naming a pass, as it can happen on any
        findings.errors(loop.id).map(_.message).loneElement shouldBe "Fishing is already level 2, so this step doesn't run"
      }
    }

    "says where a problem with a step inside another repeated step happened" in {
      // 5 feathers, 3 a rep. Level 3 is 174 xp, met on the fourth rep of 50 xp, by when the
      // second rep has taken the feathers below zero. The second trip finds level 3 already met.
      val stock = step("stock", Repetitions.once, addFeathers(5))
      val trip = step("trip", Repetitions.Fixed(2))
      val condition = StopCondition.And(feathersAtMost(0, stopBefore = true), fishingLevel(3))
      val loop = step("loop", until(condition), addFeathers(-3), fishing(50))
      find(forest(stock -> None, trip -> None, loop -> Some(trip))).map { findings =>
        LoopMessages.forPasses(findings.loops(loop.id), condition, "loop", cache.items) shouldBe List(
          LoopMessages.Problem("Feather runs out before this step stops", List("trip (1st time) › loop (2nd time)")),
          LoopMessages.Problem(
            "Feather has already run out and Fishing is already level 3, so this step doesn't run",
            List("trip (2nd time)")
          )
        )
      }
    }

    "doesn't say where a problem with a step that isn't inside another repeated step happened" in {
      val loop = step("loop", until(fishingLevel(2)), Effect.GainExp(Skill.Cooking, 1, Exp(50)))
      find(forest(loop -> None)).map(findings =>
        LoopMessages.forPasses(findings.loops(loop.id), fishingLevel(2), "loop", cache.items).map(_.where) shouldBe List(List.empty)
      )
    }

    "prefixes a repeated step's own problems with the repetition they're on" in {
      // One feather, for the first of two reps
      val stock = step("stock", Repetitions.once, addFeathers(1))
      val loop = step("loop", until(fishingLevel(2)), fishing(50), addFeathers(-1))
      find(forest(stock -> None, loop -> None)).map(
        _.errors(loop.id).map(_.message).loneElement should startWith("2nd time: ")
      )
    }

    "an effect gaining xp to a target" - {
      def toLevel(level: Int): Effect.GainExpToTarget =
        Effect.GainExpToTarget(Skill.Fishing, ExpTarget.AtLevel(Level(level)), expEach = None)

      def conflicts(findings: StepErrorFinder.Findings, step: Step): List[StepError] =
        findings.errors.getOrElse(step.id, List.empty).filter(_.message.startsWith("This reaches"))

      "is a problem when it reaches the step's target on the first rep" in {
        val effect = toLevel(3)
        val loop = step("loop", until(fishingLevel(2)), effect)
        find(forest(loop -> None)).map(
          conflicts(_, loop) shouldBe List(
            StepError(StepError.Source.Effect(0, effect), "This reaches level 2 the first time the step runs, so the step won't repeat")
          )
        )
      }

      "is a problem when the target stops the step on its own, among others" in {
        val loop = step("loop", until(StopCondition.Or(feathersAtMost(0, stopBefore = false), fishingLevel(2))), toLevel(2))
        find(forest(loop -> None)).map(conflicts(_, loop).size shouldBe 1)
      }

      "isn't a problem when it falls short of the step's target" in {
        val loop = step("loop", until(fishingLevel(3)), toLevel(2))
        find(forest(loop -> None)).map(conflicts(_, loop) shouldBe List.empty)
      }

      "isn't a problem when the target only stops the step along with another" in {
        val loop = step("loop", until(StopCondition.And(feathersAtMost(0, stopBefore = false), fishingLevel(2))), toLevel(2))
        find(forest(loop -> None)).map(conflicts(_, loop) shouldBe List.empty)
      }

      "isn't a problem for another skill" in {
        val loop = step("loop", until(fishingLevel(2)), Effect.GainExpToTarget(Skill.Hunter, ExpTarget.AtLevel(Level(5)), None))
        find(forest(loop -> None)).map(conflicts(_, loop) shouldBe List.empty)
      }
    }

    "projects a step inside one that repeats until a condition is met" - {
      val loop = step("loop", until(fishingLevel(2)))
      val first = step("first", Repetitions.once, fishing(20))
      val focus = step("focus", Repetitions.once, fishing(10))
      val f = forest(loop -> None, first -> Some(loop), focus -> Some(loop))

      "showing the first pass before it" in {
        project(f, focus).map(projection => fishingXp(projection.playerBeforeStep) shouldBe 20)
      }

      "and every pass after all its repetitions" in {
        // 30 xp a rep: 3 reps
        project(f, focus).map(projection => fishingXp(projection.playerAfterAllReps) shouldBe 90)
      }
    }

    "times a step by the repetitions it plays" in {
      // 3 reps of 1 minute, then a step after it
      val loop = step("loop", until(fishingLevel(2))).deepCopy(directEffects = EffectList(List(fishing(30))), duration = Duration.seconds(60))
      val after = step("after", Repetitions.once).deepCopy(duration = Duration.seconds(120))
      find(forest(loop -> None, after -> None)).map { findings =>
        val timing = findings.timings(loop.id)
        timing.start shouldBe Some(ScalaDuration.Zero)
        timing.durationPerRep shouldBe Some(1.minute)
        timing.firstPass shouldBe Some(3.minutes)
        timing.total shouldBe Some(3.minutes)
        timing.totalReps shouldBe 3
        findings.timings(after.id).start shouldBe Some(3.minutes)
        findings.endTime shouldBe 5.minutes
      }
    }

    "times a step inside another repeated step across each of its passes" in {
      val trip = step("trip", Repetitions.Fixed(3))
      val loop = step("loop", until(fishingLevel(2))).deepCopy(directEffects = EffectList(List(fishing(50))), duration = Duration.seconds(60))
      find(forest(trip -> None, loop -> Some(trip))).map { findings =>
        // 2 reps, then none on the next two passes
        findings.timings(loop.id).firstPass shouldBe Some(2.minutes)
        findings.timings(loop.id).total shouldBe Some(2.minutes)
        findings.timings(loop.id).totalReps shouldBe 2
        findings.timings(loop.id).insideLoop shouldBe true
        findings.timings(trip.id).total shouldBe Some(2.minutes)
      }
    }

    "doesn't count a repetition it stops before" in {
      val stock = step("stock", Repetitions.once, addFeathers(10))
      val loop = step("loop", until(feathersAtMost(0, stopBefore = true)), addFeathers(-3)).deepCopy(duration = Duration.seconds(60))
      find(forest(stock -> None, loop -> None)).map(findings =>
        findings.timings(loop.id).total shouldBe Some(3.minutes)
      )
    }

    "repeats until enough time has passed in the step" in {
      val first = step("first", Repetitions.once).deepCopy(duration = Duration.seconds(600))
      val inStep = StopCondition.TimeElapsed(StopCondition.TimeOrigin.StepStart, Duration.seconds(300), stopBefore = false)
      val loop = step("loop", until(inStep)).deepCopy(duration = Duration.seconds(60))
      find(forest(first -> None, loop -> None)).map(
        _.loops(loop.id) shouldBe List(LoopRun(5, LoopRun.Outcome.Met(List(inStep)), List.empty))
      )
    }

    "repeats until the plan reaches a time, stopping before it would pass it" in {
      // Starts at 10 minutes, 2 minutes a rep: a second rep would end at 14
      val first = step("first", Repetitions.once).deepCopy(duration = Duration.seconds(600))
      val inPlan = StopCondition.TimeElapsed(StopCondition.TimeOrigin.PlanStart, Duration.seconds(780), stopBefore = true)
      val loop = step("loop", until(inPlan)).deepCopy(duration = Duration.seconds(120))
      val after = step("after", Repetitions.once)
      find(forest(first -> None, loop -> None, after -> None)).map { findings =>
        findings.loops(loop.id) shouldBe List(LoopRun(1, LoopRun.Outcome.StoppedBefore(List(inPlan)), List.empty))
        findings.timings(after.id).start shouldBe Some(12.minutes)
      }
    }

    "counts a substep's time as time in the step" in {
      val inStep = StopCondition.TimeElapsed(StopCondition.TimeOrigin.StepStart, Duration.seconds(300), stopBefore = false)
      val loop = step("loop", until(inStep))
      val child = step("child", Repetitions.once).deepCopy(duration = Duration.seconds(120))
      find(forest(loop -> None, child -> Some(loop))).map(
        // 2, 4, then 6 minutes
        _.loops(loop.id) shouldBe List(LoopRun(3, LoopRun.Outcome.Met(List(inStep)), List.empty))
      )
    }

    "plays a step that takes no time once when repeating until time passes" in {
      val inStep = StopCondition.TimeElapsed(StopCondition.TimeOrigin.StepStart, Duration.seconds(300), stopBefore = false)
      val loop = step("loop", until(inStep))
      find(forest(loop -> None)).map(
        _.loops(loop.id) shouldBe List(LoopRun(1, LoopRun.Outcome.NoProgress, List.empty))
      )
    }

    "projects one pass of a step whose condition is already met, for context" in {
      val stock = step("stock", Repetitions.once, fishing(100))
      val loop = step("loop", until(fishingLevel(2)))
      val focus = step("focus", Repetitions.once, fishing(10))
      val f = forest(stock -> None, loop -> None, focus -> Some(loop))
      project(f, focus).map { projection =>
        fishingXp(projection.playerBeforeStep) shouldBe 100
        fishingXp(projection.playerAfterAllReps) shouldBe 110
      }
    }

    "projects the rest of the plan after a step that stopped before passing its condition" in {
      val stock = step("stock", Repetitions.once, addFeathers(10))
      val loop = step("loop", until(feathersAtMost(0, stopBefore = true)))
      val use = step("use", Repetitions.once, addFeathers(-3))
      val after = step("after", Repetitions.once)
      val f = forest(stock -> None, loop -> None, use -> Some(loop), after -> None)
      for {
        before <- project(f, after)
        inside <- project(f, use)
      } yield {
        featherCount(before.playerBeforeStep) shouldBe 1
        // The last rep played is the third, which starts with 4 feathers
        featherCount(inside.playerAfterAllReps) shouldBe 1
      }
    }
  }
}
