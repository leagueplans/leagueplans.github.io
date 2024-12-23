package ddm.ui.dom.common

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, eventPropToProcessor, seqToModifier}
import ddm.ui.facades.animation.KeyframeAnimationOptions
import ddm.ui.utils.laminar.LaminarOps.onMountAnimate
import ddm.ui.wrappers.animation.{KeyframeProperty, animate}
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Tooltip {
  def apply(contents: L.HtmlElement): L.Modifier[L.HtmlElement] = {
    val mouseOver = Var(false)
    val mouseCoords = Var((0.0, 0.0))
    val isVisible = Var(false)
    val visibilityStream = toVisibilityStream(mouseOver.signal, mouseCoords.signal, isVisible.signal)

    List(
      L.cls(Styles.hasTooltip),
      mouseEventListeners(mouseOver.writer, mouseCoords.writer),
      L.child.maybe <-- visibilityStream.splitOption((_, coords) =>
        contents.amend(tooltipModifiers(coords))
      ),
      visibilityStream.map(_.nonEmpty) --> isVisible
    )
  }

  @js.native @JSImport("/styles/common/tooltip.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val hasTooltip: String = js.native
    val tooltip: String = js.native
  }

  private def mouseEventListeners(
    mouseOver: Observer[Boolean],
    mouseCoords: Observer[(Double, Double)]
  ): L.Modifier[L.HtmlElement] =
    List(
      L.inContext[L.HtmlElement](node =>
        L.onMouseOver --> mouseOver.contracollect[MouseEvent] {
          case event => isClosestListener(event, node.ref)
        }
      ),
      L.onMouseLeave --> mouseOver.contramap[MouseEvent](_ => false),
      L.onMouseMove --> mouseCoords.contramap[MouseEvent](event => (event.clientX, event.clientY))
    )

  private def isClosestListener(event: MouseEvent, parent: Element): Boolean =
    event.target.isInstanceOf[Element] &&
      Option(event.target.asInstanceOf[Element].closest(s".${Styles.hasTooltip}")).contains(parent)

  private def tooltipModifiers(mouseCoords: Signal[(Double, Double)]): L.Modifier[L.HtmlElement] =
    List(
      L.cls(Styles.tooltip),
      L.left <-- toOffset(mouseCoords)(_._1 + 12), // the +12 is to offset from the cursor
      L.top <-- toOffset(mouseCoords)(_._2 - 10),
      L.onMountAnimate(_.animate(
        KeyframeAnimationOptions(
          duration = 250,
          easing = "ease-in-out"
        ),
        KeyframeProperty.opacity(0, 1)
      ))
    )

  private def toVisibilityStream(
    mouseOver: Signal[Boolean],
    mouseCoords: Signal[(Double, Double)],
    isVisible: Signal[Boolean]
  ): EventStream[Option[(Double, Double)]] = {
    val showEvents =
      mouseCoords
        .changes
        .debounce(ms = 400)
        .withCurrentValueOf(mouseOver, isVisible)
        .collect { case (x, y, true, false) => Some((x, y)) }

    val hideEvents = mouseOver.changes.collect { case false => None }

    EventStream.merge(showEvents, hideEvents)
  }

  private def toOffset(
    visibility: Signal[(Double, Double)]
  )(pick: ((Double, Double)) => Double): Signal[String] =
    visibility.map(coords => s"${pick(coords)}px")
}
