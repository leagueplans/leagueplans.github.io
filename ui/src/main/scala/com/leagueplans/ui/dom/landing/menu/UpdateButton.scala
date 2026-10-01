package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.ExportedPlanDecoder
import com.leagueplans.ui.storage.client.StorageClient
import com.leagueplans.ui.storage.migrations.MigrationError
import com.leagueplans.ui.storage.model.errors.{DeletionError, FileSystemError}
import com.leagueplans.ui.storage.model.{PlanExport, PlanID, PlanMetadata}
import com.leagueplans.uicommon.dom.{Button, ToastHub}
import com.leagueplans.uicommon.utils.airstream.EventStreamOps.andThen
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, textToTextNode}
import org.scalajs.dom.window

import scala.concurrent.duration.{Duration, DurationInt}

object UpdateButton {
  def apply(
    id: PlanID,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): L.Button = {
    val clickStream = EventBus[Unit]()

    Button(_.handled --> clickStream.writer).amend(
      AsyncButtonModifiers(
        "Update",
        clickStream.events.flatMapWithStatus(
          onClick(id, storage, toastPublisher)
        )
      )
    )
  }

  private def onClick(
    id: PlanID,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] = {
    val exportPromise = storage.fetch(id).changes.collectSome
    exportPromise
      .andThen[DecodingFailure | MigrationError | FileSystemError, (PlanMetadata, Plan)](ExportedPlanDecoder.decode)
      .andThen((metadata, plan) => storage.create(metadata, plan).changes.collectSome)
      .andThen(_ => storage.delete(id).changes.collectSome)
      .map { result =>
        result match {
          case Right(_) =>
            toastPublisher.publish(
              ToastHub.Type.Success,
              5.seconds,
              "Updated plan to the latest save format"
            )

          case Left(error: DecodingFailure) =>
            publishBug("Unexpected error reading plan", error.getMessage, toastPublisher)

          case Left(error: MigrationError) =>
            publishBug("Unexpected error updating plan to the latest save format", error.message, toastPublisher)

          case Left(error: FileSystemError) =>
            toastPublisher.publish(
              ToastHub.Type.Error,
              15.seconds,
              "Couldn't update plan",
              Some(error.message)
            )

          case Left(error: DeletionError) =>
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              "Updated plan, but couldn't delete the old copy",
              Some(error.message)
            )
        }
        ()
      }
  }

  // These stay until dismissed, since we want the user to have time to report them
  private def publishBug(title: String, cause: String, toastPublisher: ToastHub.Publisher): Unit =
    toastPublisher.publish(
      ToastHub.Type.Error,
      Duration.Inf,
      title,
      Some(s"Please report this to @Granarder on Discord. Cause: $cause"),
      Some(ToastHub.Action(
        "Copy error details",
        () => window.navigator.clipboard.writeText(s"$title\n$cause").`then`[Unit](_ =>
          toastPublisher.publish(ToastHub.Type.Success, 2500.milliseconds, "Copied error details")
        ): Unit
      ))
    )
}
