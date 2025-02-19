package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.dom.common.{Button, ToastHub}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.ExportedPlanDecoder
import com.leagueplans.ui.storage.client.StorageClient
import com.leagueplans.ui.storage.migrations.MigrationError
import com.leagueplans.ui.storage.model.errors.{DeletionError, FileSystemError}
import com.leagueplans.ui.storage.model.{PlanExport, PlanID, PlanMetadata}
import com.leagueplans.ui.utils.airstream.EventStreamOps.andThen
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, textToTextNode}

import scala.concurrent.duration.DurationInt

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
              ToastHub.Type.Info,
              5.seconds,
              "Successfully updated plan to the latest save file format"
            )

          case Left(error: DecodingFailure) =>
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              s"Unexpected error decoding plan." +
                s" Please report this to @Granarder via discord. Cause: [${error.getMessage}]"
            )

          case Left(error: MigrationError) =>
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              s"Unexpected error migrating plan to the latest save file format." +
                s" Please report this to @Granarder via discord. Cause: [${error.message}]"
            )

          case Left(error: FileSystemError) =>
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              s"Failed to update plan. Cause: [${error.message}]"
            )

          case Left(error: DeletionError) =>
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              s"Successfully updated plan, but failed to delete the old copy. Cause [${error.message}]"
            )
        }
        ()
      }
  }
}
