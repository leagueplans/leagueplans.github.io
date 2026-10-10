package com.leagueplans.ui.storage.client

import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.model.errors.SubscriptionError
import com.leagueplans.ui.storage.model.{LoadProgress, PlanID}
import com.raquo.airstream.state.StrictSignal

/** A subscription that's still being set up. Reading a big plan can take seconds, so this reports
  * progress and can be cancelled. */
final class PendingSubscription(
  val planID: PlanID,
  /** None until the plan has been read */
  val result: StrictSignal[Option[Either[SubscriptionError, (Plan, PlanSubscription)]]],
  /** None until the plan's size is known */
  val progress: StrictSignal[Option[LoadProgress]],
  onCancel: () => Unit
) {
  /** Abandons the subscription, if it hasn't finished already, setting the result to
    * [[SubscriptionError.Cancelled]] */
  def cancel(): Unit =
    onCancel()
}
