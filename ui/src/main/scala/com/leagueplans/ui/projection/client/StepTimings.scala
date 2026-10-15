package com.leagueplans.ui.projection.client

import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.projection.model.StepTiming
import com.raquo.airstream.core.Signal

import scala.concurrent.duration.Duration

/** When each step happens, as last worked out by the projection worker. The timings catch up with
  * the plan a moment after it changes. */
final class StepTimings(all: Signal[Map[Step.ID, StepTiming]], val endTime: Signal[Duration]) {
  def get(step: Step.ID): Signal[StepTiming] =
    all.map(_.getOrElse(step, StepTiming.unknown)).distinct
}
