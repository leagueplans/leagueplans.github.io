package com.leagueplans.uicommon.dom

import com.leagueplans.uicommon.facades.animation.{AnimationPlayState, FillMode, KeyframeAnimationOptions}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.facades.fontawesome.commontypes.IconDefinition
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handled
import com.leagueplans.uicommon.wrappers.animation.{Animation, KeyframeProperty}
import com.raquo.airstream.core.{Observer, Signal, Sink}
import com.raquo.airstream.eventbus.{EventBus, WriteBus}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor}
import org.scalajs.dom.{Event, window}

import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ToastHub {
  enum Type { case Info, Success, Warning, Error }

  final case class Toast(`type`: Type, duration: Duration, content: L.Node)

  final class Publisher(underlying: WriteBus[Toast]) extends Sink[Toast] {
    export underlying.toObserver

    def publish(`type`: Type, duration: Duration, content: L.Node): Unit =
      publish(Toast(`type`, duration, content))

    def publish(toast: Toast): Unit =
      underlying.onNext(toast)
  }

  def apply(tooltip: Tooltip): (L.Div, Publisher) = {
    val bus = EventBus[Toast]()
    val activeToastVar = Var(List.empty[Entry])
    val filterer = activeToastVar.updater[Entry]((acc, entry) => acc.filterNot(_ == entry))

    val node = L.div(
      L.cls(Styles.toastHub),
      L.children <-- activeToastVar.signal.split(identity)((_, entry, _) =>
        toNode(entry.toast, filterer.contramap[Unit](_ => entry), tooltip)
      ),
      // Newest toasts go last, so that they appear closest to the corner of the screen
      bus.events.withCurrentValueOf(activeToastVar)
        .map((toast, acc) => acc :+ Entry(toast)) --> activeToastVar
    )

    (node, Publisher(bus.writer))
  }

  /* Entries are compared by reference, so that publishing the same toast twice results
   * in two toasts that are displayed and dismissed independently
   */
  private final class Entry(val toast: Toast)

  @js.native @JSImport("/styles/common/toastHub.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val toastHub: String = js.native
    val slot: String = js.native
    val leaving: String = js.native
    val icon: String = js.native
    val content: String = js.native
    val info: String = js.native
    val success: String = js.native
    val warning: String = js.native
    val error: String = js.native
    val dismiss: String = js.native
    val countdown: String = js.native
  }

  /* A toast's animations are scripted rather than CSS animations. The hub is moved into and
   * out of the modal when it opens and closes, and browsers restart CSS animations on
   * elements that are reinserted into the DOM. Scripted animations carry on regardless.
   *
   * They're played as soon as they're needed rather than on mount, since moving the hub may
   * unmount a toast.
   */
  private val slideIn = Animation(
    new KeyframeAnimationOptions {
      duration = 280
      easing = "cubic-bezier(0.2, 0.9, 0.3, 1)"
    },
    KeyframeProperty.opacity(0, 1),
    KeyframeProperty.transform("translateX(calc(100% + 1rem))", "translateX(0)")
  )

  // Collapses the slot's height too, so that the toasts below move up smoothly
  private val slideOut = Animation(
    new KeyframeAnimationOptions {
      duration = 200
      easing = "ease-in"
      fill = FillMode.forwards
    },
    KeyframeProperty.gridTemplateRows("1fr", "0fr"),
    KeyframeProperty.paddingTop("0.5rem", "0"),
    KeyframeProperty.opacity(1, 0),
    KeyframeProperty.transform("translateX(0)", "translateX(1.5rem)")
  )

  private val fadeOut = Animation(
    new KeyframeAnimationOptions {
      duration = 120
      fill = FillMode.forwards
    },
    KeyframeProperty.opacity(1, 0)
  )

  /* Dismissal happens in two phases. The countdown finishing or the user clicking the
   * dismiss button plays an exit animation. The toast is only removed from the hub once that
   * animation has finished.
   */
  private def toNode(
    toast: Toast,
    removalObserver: Observer[Unit],
    tooltip: Tooltip
  ): L.Div = {
    val leaving = Var(false)
    val (style, icon, role) = typeSpecificAttributes(toast.`type`)

    // Give the user time to read, or to reach for a button
    val hovered = Var(false)
    val focused = Var(false)
    val paused = Signal.combine(hovered.signal, focused.signal, leaving.signal).map(_ || _ || _)

    // The slot and the dismissal observer refer to each other, hence the lazy vals
    lazy val dismiss: Observer[Unit] = Observer { _ =>
      if (!leaving.now()) {
        leaving.set(true)
        val exit = (if (prefersReducedMotion) fadeOut else slideOut).play(slot)
        exit.onfinish = (_ => removalObserver.onNext(())): js.Function1[Event, Unit]
        // Otherwise a cancelled exit would leave the toast on screen for good
        exit.oncancel = (_ => removalObserver.onNext(())): js.Function1[Event, Unit]
      }
    }

    lazy val toastElement: L.Div = L.div(
      L.cls(style),
      L.role(role),
      FontAwesome.icon(icon).amend(L.svg.cls(Styles.icon)),
      L.div(L.cls(Styles.content), toast.content),
      dismissButton(dismiss, tooltip),
      countdown(toast.duration, paused, dismiss)
    )
    if (!prefersReducedMotion) slideIn.play(toastElement): Unit

    lazy val slot: L.Div = L.div(
      L.cls(Styles.slot),
      L.cls(Styles.leaving) <-- leaving.signal,
      L.onMouseEnter.mapTo(true) --> hovered,
      L.onMouseLeave.mapTo(false) --> hovered,
      // Focus events don't bubble, so we listen for them during the capture phase instead
      L.onFocus.useCapture.mapTo(true) --> focused,
      L.onBlur.useCapture.mapTo(false) --> focused,
      toastElement
    )
    slot
  }

  private def prefersReducedMotion: Boolean =
    window.matchMedia("(prefers-reduced-motion: reduce)").matches

  private def typeSpecificAttributes(`type`: Type): (String, IconDefinition, String) =
    `type` match {
      case Type.Info => (Styles.info, FreeSolid.faCircleInfo, "status")
      case Type.Success => (Styles.success, FreeSolid.faCircleCheck, "status")
      case Type.Warning => (Styles.warning, FreeSolid.faTriangleExclamation, "alert")
      case Type.Error => (Styles.error, FreeSolid.faCircleExclamation, "alert")
    }

  private def dismissButton(observer: Observer[Unit], tooltip: Tooltip): L.Button =
    Button(_.handled --> observer).amend(
      L.cls(Styles.dismiss),
      FontAwesome.icon(FreeSolid.faXmark),
      IconButtonModifiers(
        tooltipContents = "Dismiss",
        screenReaderDescription = "dismiss",
        tooltip,
        tooltipPlacement = Placement.left
      )
    )

  // Scripted for the same reason as the toast's other animations
  private def countdown(
    duration: Duration,
    paused: Signal[Boolean],
    expiryObserver: Observer[Unit]
  ): L.Modifier[L.HtmlElement] =
    duration match {
      case _: Duration.Infinite => L.emptyMod
      case finiteDuration: FiniteDuration =>
        val element = L.div(L.cls(Styles.countdown))
        val animation = Animation(
          new KeyframeAnimationOptions {
            this.duration = finiteDuration.toMillis.toDouble
            fill = FillMode.forwards
          },
          KeyframeProperty.transform("scaleX(1)", "scaleX(0)")
        ).play(element)
        animation.onfinish = (_ => expiryObserver.onNext(())): js.Function1[Event, Unit]

        element.amend(
          paused --> { isPaused =>
            if (isPaused) animation.pause()
            // Calling play on a finished animation would restart it
            else if (animation.playState == AnimationPlayState.paused) animation.play()
          }
        )
    }
}
