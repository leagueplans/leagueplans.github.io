package ddm.ui.dom.plan

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L
import ddm.ui.dom.common.Button
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.utils.laminar.FontAwesome
import ddm.ui.utils.laminar.LaminarOps.handledAs

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object CollapsibleList {
  def apply(
    countSignal: Signal[Int],
    showInitially: Boolean,
    contentType: String,
    list: L.HtmlElement
  ): Signal[L.Node] = {
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
    Button(show)(_.handledAs(true)).amend(
      L.cls(Styles.listToggleHorizontal),
      FontAwesome.icon(FreeSolid.faCaretRight),
      L.span(
        L.cls(Styles.collapsedListAnnotation),
        L.child.text <-- countSignal.map(i => s"$i ${contentType}s hidden")
      )
    )

  private def collapseListButton(showSubSteps: Observer[Boolean]): L.Button =
    Button(showSubSteps)(_.handledAs(false)).amend(
      L.cls(Styles.listToggleVertical),
      FontAwesome.icon(FreeSolid.faCaretDown),
      L.div(L.cls(Styles.collapseBanner))
    )
}
