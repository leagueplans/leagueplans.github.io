package ddm.ui.dom.common

import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, seqToModifier}
import ddm.ui.facades.fontawesome.commontypes.IconDefinition
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.RichEventProp

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ToastHub {
  sealed trait Type

  object Type {
    case object Info extends Type
    case object Warning extends Type
    case object Error extends Type
  }

  final case class Toast(`type`: Type, duration: Duration, content: L.Node)

  def apply(): (L.Div, WriteBus[Toast]) = {
    val bus = new EventBus[Toast]
    val activeToastVar = Var(List.empty[Toast])
    val filterer = activeToastVar.updater[Toast] { case (acc, toast) => acc.filterNot(_ == toast) }

    val node = L.div(
      L.cls(Styles.toastHub),
      L.children <-- activeToastVar.signal.split(identity) { case (_, toast, _) =>
        toNode(toast, filterer.contramap[Unit](_ => toast))
      },
      bus.events.withCurrentValueOf(activeToastVar)
        .map { case (toast, acc) => toast +: acc } --> activeToastVar,
    )

    (node, bus.writer)
  }

  @js.native @JSImport("/styles/common/toastHub.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val toastHub: String = js.native
    val icon: String = js.native
    val info: String = js.native
    val warning: String = js.native
    val error: String = js.native
    val dismiss: String = js.native
  }

  private def toNode(toast: Toast, dismissObserver: Observer[Unit]): L.Div =
    L.div(
      toast.`type` match {
        case Type.Info => typeSpecificAttributes(Styles.info, FreeSolid.faCircleInfo)
        case Type.Warning => typeSpecificAttributes(Styles.warning, FreeSolid.faTriangleExclamation)
        case Type.Error => typeSpecificAttributes(Styles.error, FreeSolid.faCircleExclamation)
      },
      toast.content,
      dismissButton(dismissObserver),
      autoDismiss(toast.duration) --> dismissObserver
    )

  private def typeSpecificAttributes(
    style: String,
    icon: IconDefinition
  ): List[L.Modifier[L.HtmlElement]] =
    List(
      L.cls(style),
      FontAwesome.icon(icon).amend(L.svg.cls(Styles.icon))
    )

  private def dismissButton(observer: Observer[Unit]): L.Button =
    L.button(
      L.cls(Styles.dismiss),
      L.`type`("button"),
      FontAwesome.icon(FreeSolid.faXmark),
      L.onClick.handled --> observer
    )

  private def autoDismiss(duration: Duration): EventStream[Unit] =
    duration match {
      case _: Duration.Infinite => EventStream.empty
      case duration: FiniteDuration => EventStream.unit().delay(duration.toMillis.toInt)
    }
}
