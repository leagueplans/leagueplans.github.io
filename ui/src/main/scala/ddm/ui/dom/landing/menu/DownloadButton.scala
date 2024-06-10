package ddm.ui.dom.landing.menu

import com.raquo.airstream.core.EventStream
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.codec.encoding.Encoder
import ddm.ui.dom.common.ToastHub
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.storage.client.StorageClient
import ddm.ui.storage.model.{PlanExport, PlanID}
import ddm.ui.utils.laminar.FontAwesome
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
  ): L.Button =
    AsyncButton(
      FontAwesome.icon(FreeSolid.faDownload),
      () => onClick(id, name, storage, toastPublisher)
    )

  private def onClick(
    id: PlanID,
    name: String,
    storage: StorageClient,
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] =
    storage.fetch(id).changes.collectSome.flatMap {
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
