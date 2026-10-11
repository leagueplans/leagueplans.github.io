package com.leagueplans.ui.dom.plans.actions

import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.storage.client.StorageClient
import com.leagueplans.ui.storage.model.{PlanExport, PlanID}
import com.leagueplans.uicommon.dom.ToastHub
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.state.StrictSignal
import com.raquo.laminar.api.L
import org.scalajs.dom.*

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.JSConverters.JSRichIterable
import scala.scalajs.js.typedarray.AB2TA

/** Downloads the plan as a gzipped file, named after the plan as it is when the button is pressed */
object DownloadButton {
  def apply(
    id: PlanID,
    name: StrictSignal[String],
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): L.Button =
    AsyncButton("Download plan file", () => onClick(id, name.now(), storage, toastPublisher))

  private def onClick(
    id: PlanID,
    name: String,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] =
    storage.fetch(id).changes.collectSome.flatMapSwitch {
      case Left(error) =>
        toastPublisher.publish(
          ToastHub.Type.Error,
          15.seconds,
          "Couldn't prepare download",
          Some(error.message)
        )
        EventStream.fromValue(())

      case Right(plan) =>
        compress(plan).map(triggerDownload(name, _))
    }

  private def compress(plan: PlanExport): EventStream[Blob] = {
    val stream =
      new Blob(List(Encoder.encode(plan).getBytes.toTypedArray).toJSIterable)
        .stream()
        .pipeThrough(new CompressionStream(CompressionFormat.gzip))

    EventStream.fromJsPromise(new Response(stream).blob())
  }

  private def triggerDownload(name: String, data: Blob): Unit = {
    val url = URL.createObjectURL(data)
    L.a(L.href(url), L.download(s"$name.plan.gz")).ref.click()
    URL.revokeObjectURL(url)
  }
}
