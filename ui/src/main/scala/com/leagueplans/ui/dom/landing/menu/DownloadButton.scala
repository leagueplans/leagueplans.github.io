package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers, ToastHub}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.storage.client.StorageClient
import com.leagueplans.ui.storage.model.{PlanExport, PlanID}
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, textToTextNode}
import org.scalajs.dom.*

import scala.concurrent.duration.DurationInt
import scala.scalajs.js.JSConverters.JSRichIterable
import scala.scalajs.js.typedarray.AB2TA

object DownloadButton {
  def apply(
    id: PlanID,
    name: String,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): L.Button = {
    val clickStream = EventBus[Unit]()

    Button(_.handled --> clickStream.writer).amend(
      IconButtonModifiers(
        tooltip = "Download",
        screenReaderDescription = "download"
      ),
      AsyncButtonModifiers(
        FontAwesome.icon(FreeSolid.faDownload),
        clickStream.events.flatMapWithStatus(
          onClick(id, name, storage, toastPublisher)
        )
      )
    )
  }

  private def onClick(
    id: PlanID,
    name: String,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] =
    storage.fetch(id).changes.collectSome.flatMapSwitch {
      case Left(error) =>
        toastPublisher.publish(
          ToastHub.Type.Warning,
          15.seconds,
          s"Failed to prepare download. Cause: [${error.message}]"
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
