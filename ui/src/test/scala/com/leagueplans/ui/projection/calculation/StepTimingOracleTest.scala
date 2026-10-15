package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.{Forest, ForestPropertyTest}
import com.leagueplans.ui.model.plan.{Repetitions, Step}
import com.leagueplans.ui.projection.calculation.loops.{LoopContext, Shortfalls}
import com.leagueplans.ui.projection.model.StepTiming
import com.leagueplans.uicommon.utils.scala.DurationOps.safeMul
import org.scalacheck.Gen
import org.scalacheck.rng.Seed
import org.scalajs.dom.AbortController
import org.scalajs.macrotaskexecutor.MacrotaskExecutor
import org.scalatest.Assertion
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/** The worker times steps as it plays them. For steps that repeat a fixed number of times, that
  * must agree with the old TimeKeeper, which worked the timings out from the shape of the plan. */
final class StepTimingOracleTest extends AsyncFreeSpec with Matchers {
  override implicit val executionContext: ExecutionContext = MacrotaskExecutor.Implicits.global

  private val looping = new ForestFolder.Looping[Step, Unit] {
    def repetitions(step: Step): Repetitions = step.repetitions
    def duration(step: Step): Option[Duration] = step.duration.toOption.map(_.asScala)
    def context(acc: Unit, shortfalls: Shortfalls): LoopContext = throw IllegalStateException("No conditions")
    def shortfalls(acc: Unit, step: Step, reps: Int): Shortfalls = Shortfalls.none
  }

  private def timings(forest: Forest[Step.ID, Step]): Future[(Map[Step.ID, StepTiming], Duration)] =
    ForestFolder.foldLeftAsync(forest, (), new AbortController().signal, looping)((_, _, _) => ()).map { maybe =>
      val result = maybe.get
      (
        result.timings.map((id, timing) => id -> timing.copy(insideLoop = StepErrorFinder.insideLoop(forest, Map.empty, id))),
        result.elapsed.getOrElse(Duration.Zero)
      )
    }

  private def check(forest: Forest[Step.ID, Step]): Future[Assertion] = {
    val oracle = TimeKeeper(forest)
    timings(forest).map { (worked, endTime) =>
      withClue(s"$forest:") {
        endTime shouldEqual oracle.endTime.now()
        worked.keySet shouldEqual forest.nodes.keySet
      }
      forest.nodes.keys.foreach { id =>
        val expected = oracle.get(id).now()
        val actual = worked(id)
        val ancestorReps = forest.ancestors(id).flatMap(forest.get).map(_.repetitions.assumed).product
        withClue(s"$id in $forest:") {
          actual.start shouldEqual expected.start
          actual.durationPerRep shouldEqual expected.durationPerRep
          actual.firstPass shouldEqual expected.durationPerParentRep
          actual.finish shouldEqual expected.finish
          actual.insideLoop shouldEqual expected.insideLoop
          actual.total shouldEqual expected.durationPerParentRep.map(_.safeMul(ancestorReps))
          actual.totalReps shouldEqual expected.repetitions * ancestorReps
        }
      }
      succeed
    }
  }

  "Step timings" - {
    "agree with working them out from the shape of the plan" in {
      val samples = Gen.listOfN(200, ForestPropertyTest.opsGen).pureApply(Gen.Parameters.default, Seed(15102026))
      Future.traverse(samples)(ops => check(ForestPropertyTest.resolveAll(ops))).map(_ => succeed)
    }
  }
}
