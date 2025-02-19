package com.leagueplans.ui.dom.common

import com.leagueplans.ui.facades.fontawesome.commontypes.IconDefinition
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{EventStream, Observer, Sink}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, seqToModifier}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ToastHub {
  enum Type { case Info, Warning, Error }

  final case class Toast(`type`: Type, duration: Duration, content: L.Node)
  
  final class Publisher(underlying: WriteBus[Toast]) extends Sink[Toast] {
    export underlying.toObserver
    
    def publish(`type`: Type, duration: Duration, content: L.Node): Unit =
      publish(Toast(`type`, duration, content))
    
    def publish(toast: Toast): Unit =
      underlying.onNext(toast)
  }

  def apply(): (L.Div, Publisher) = {
    val bus = EventBus[Toast]()
    val activeToastVar = Var(List.empty[Toast])
    val filterer = activeToastVar.updater[Toast]((acc, toast) => acc.filterNot(_ == toast))

    val node = L.div(
      L.cls(Styles.toastHub),
      L.children <-- activeToastVar.signal.split(identity)((_, toast, _) =>
        toNode(toast, filterer.contramap[Unit](_ => toast))
      ),
      bus.events.withCurrentValueOf(activeToastVar)
        .map((toast, acc) => toast +: acc) --> activeToastVar,
    )

    (node, Publisher(bus.writer))
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
    Button(_.handled --> observer).amend(
      L.cls(Styles.dismiss),
      FontAwesome.icon(FreeSolid.faXmark),
      IconButtonModifiers(
        tooltip = "Dismiss",
        screenReaderDescription = "dismiss"
      )
    )

  private def autoDismiss(duration: Duration): EventStream[Unit] =
    duration match {
      case _: Duration.Infinite => EventStream.empty
      case duration: FiniteDuration => EventStream.unit().delay(duration.toMillis.toInt)
    }
}
