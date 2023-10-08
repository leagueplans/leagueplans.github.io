package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor, textToNode}
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.laminar.LaminarOps.RichL
import org.scalajs.dom.MouseEvent

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object CollapsibleList {
  def apply(
    countSignal: Signal[Int],
    showInitially: Boolean,
    contentType: String,
    list: L.HtmlElement
  ): Signal[L.Child] = {
    val showSubStepsState = Var(showInitially)

    Signal
      .combine(showSubStepsState, countSignal)
      .splitOne { case (showSubSteps, subStepCount) => (showSubSteps, subStepCount != 0) } {
        case ((_, false), _, _) =>
          L.emptyNode
        case ((false, true), _, _) =>
          expandListButton(
            countSignal,
            showSubStepsState.writer,
            contentType
          )
        case ((true, true), _, _) =>
          L.div(
            L.cls(Styles.expandedList),
            collapseListButton(showSubStepsState.writer),
            list
          )
      }
  }

  @js.native @JSImport("/styles/plan/collapsibleList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val listToggleHorizontal: String = js.native
    val listToggleVertical: String = js.native
    val collapsedListAnnotation: String = js.native
    val collapseBanner: String = js.native
    val expandedList: String = js.native
  }

  private def expandListButton(
    countSignal: Signal[Int],
    show: Observer[Boolean],
    contentType: String
  ): L.Button =
    L.button(
      L.cls(Styles.listToggleHorizontal),
      L.`type`("button"),
      L.icon(FreeSolid.faCaretRight),
      L.span(
        L.cls(Styles.collapsedListAnnotation),
        L.child.text <-- countSignal.map(i => s"$i ${contentType}s hidden")
      ),
      L.ifUnhandled(L.onClick) --> show.contramap[MouseEvent] { event =>
        event.preventDefault()
        true
      }
    )

  private def collapseListButton(showSubSteps: Observer[Boolean]): L.Button =
    L.button(
      L.cls(Styles.listToggleVertical),
      L.`type`("button"),
      L.icon(FreeSolid.faCaretDown),
      L.div(L.cls(Styles.collapseBanner)),
      L.ifUnhandled(L.onClick) --> showSubSteps.contramap[MouseEvent] { event =>
        event.preventDefault()
        false
      }
    )
}
