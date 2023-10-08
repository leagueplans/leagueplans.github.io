package ddm.ui.dom.common

import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringValueMapper, enrichSource, eventPropToProcessor, seqToModifier, styleToReactiveStyle}
import ddm.ui.facades.dom.RichElement.Ops
import org.scalajs.dom.{Element, MouseEvent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Tooltip {
  def apply(contents: L.HtmlElement): L.Modifier[L.HtmlElement] = {
    val mouseOver = Var(false)
    val mouseCoords = Var((0.0, 0.0))

    List(
      parentModifiers(mouseOver.writer, mouseCoords.writer),
      contents.amend(
        tooltipSetters(mouseOver.signal, mouseCoords.signal)
      )
    )
  }

  @js.native @JSImport("/styles/common/tooltip.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val hasTooltip: String = js.native
    val hidden: String = js.native
    val visible: String = js.native
  }

  private def parentModifiers(
    mouseOver: Observer[Boolean],
    mouseCoords: Observer[(Double, Double)]
  ): L.Modifier[L.HtmlElement] =
    List(
      L.cls(Styles.hasTooltip),
      L.inContext[L.HtmlElement](node =>
        L.onMouseOver --> mouseOver.contracollect[MouseEvent] {
          case event => isClosestListener(event, node.ref, Styles.hasTooltip)
        }
      ),
      L.onMouseLeave --> mouseOver.contramap[MouseEvent](_ => false),
      L.onMouseMove --> mouseCoords.contramap[MouseEvent](event => (event.clientX, event.clientY))
    )

  private def isClosestListener(event: MouseEvent, parent: Element, cls: String): Boolean =
    event.target.isInstanceOf[Element] &&
      event.target.asInstanceOf[Element]
        .closestClass(cls)
        .contains(parent)

  private def tooltipSetters(
    mouseOver: Signal[Boolean],
    mouseCoords: Signal[(Double, Double)]
  ): L.Modifier[L.HtmlElement] = {
    val visibility = Var(false)
    val visibilityStream = toVisibilityStream(mouseOver, mouseCoords, visibility.signal)

    List(
      L.cls <-- visibilityStream.toSignal(None).map {
        case Some(_) => Styles.visible
        case None => Styles.hidden
      },
      L.left <-- toOffset(visibilityStream)(_._1 + 12), // the +12 is to offset from the cursor
      L.top <-- toOffset(visibilityStream)(_._2 - 10),
      visibilityStream.map(_.nonEmpty) --> visibility
    )
  }

  private def toVisibilityStream(
    mouseOver: Signal[Boolean],
    mouseCoords: Signal[(Double, Double)],
    visibility: Signal[Boolean]
  ): EventStream[Option[(Double, Double)]] = {
    val showEvents =
      mouseCoords
        .changes
        .debounce(ms = 400)
        .withCurrentValueOf(mouseOver, visibility)
        .collect { case (x, y, true, false) => Some((x, y)) }

    val hideEvents = mouseOver.changes.collect { case false => None }

    EventStream.merge(showEvents, hideEvents)
  }

  private def toOffset(
    visibility: EventStream[Option[(Double, Double)]]
  )(pick: ((Double, Double)) => Double): EventStream[String] =
    visibility.collect { case Some(coords) => s"${pick(coords)}px" }
}
