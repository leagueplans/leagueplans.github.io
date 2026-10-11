package com.leagueplans.ui.dom.plans.actions

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.dom.plans.PlanManager
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.ExportedPlanDecoder
import com.leagueplans.ui.storage.local.PlanLocalStorage
import com.leagueplans.ui.storage.migrations.MigrationError
import com.leagueplans.ui.storage.model.errors.FileSystemError
import com.leagueplans.ui.storage.model.{PlanID, PlanMetadata}
import com.leagueplans.uicommon.dom.ToastHub
import com.leagueplans.uicommon.utils.airstream.EventStreamOps.andThen
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.ownership.ManualOwner
import com.raquo.airstream.state.StrictSignal
import com.raquo.laminar.api.L

import scala.concurrent.duration.DurationInt

/** Saves a copy of the plan, as it is when the button is pressed, and opens the copy */
object DuplicateButton {
  def apply(
    id: PlanID,
    name: StrictSignal[String],
    manager: PlanManager,
    toastPublisher: ToastHub.Publisher
  ): L.Button =
    AsyncButton("Make a copy", () => onClick(id, copyName(name.now()), manager, toastPublisher))

  /** The name a copy of the plan is given */
  def copyName(name: String): String =
    s"$name (copy)"

  private def onClick(
    id: PlanID,
    copyName: String,
    manager: PlanManager,
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] =
    val exportPromise = manager.storage.fetch(id).changes.collectSome
    exportPromise
      .andThen[DecodingFailure | MigrationError | FileSystemError, (PlanMetadata, Plan)](ExportedPlanDecoder.decode)
      .flatMapSwitch {
        case Right((_, plan)) =>
          report(id, manager.create(PlanMetadata(copyName), plan.copy(name = copyName)), toastPublisher)

        case Left(error: DecodingFailure) =>
          fail(error.getMessage, toastPublisher)
          EventStream.fromValue(())

        case Left(error: MigrationError) =>
          fail(error.message, toastPublisher)
          EventStream.fromValue(())

        case Left(error: FileSystemError) =>
          fail(error.message, toastPublisher)
          EventStream.fromValue(())
      }

  /** Opening the copy replaces the planning page, and with it anything listening from the page,
    * so this listens from outside it. It stops once the copy is saved.
    */
  private def report(
    originalID: PlanID,
    created: StrictSignal[Option[Either[FileSystemError, PlanID]]],
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] = {
    val owner = new ManualOwner
    created.foreach {
      case Some(Right(copyID)) =>
        // The copy opens as the original was left, such as on Settings and with the same steps
        // collapsed. The copy's page reads these when it opens, which is after this.
        PlanLocalStorage.copy(originalID, copyID)
        toastPublisher.publish(ToastHub.Type.Success, 8.seconds, "Opened the copy. Rename it in Settings.")
        owner.killSubscriptions()

      case Some(Left(error)) =>
        fail(error.message, toastPublisher)
        owner.killSubscriptions()

      case None => ()
    }(using owner)
    created.changes.collectSome.mapToUnit
  }

  private def fail(reason: String, toastPublisher: ToastHub.Publisher): Unit =
    toastPublisher.publish(ToastHub.Type.Error, 15.seconds, "Couldn't duplicate plan", Some(reason))
}
