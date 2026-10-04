package com.leagueplans.ui.dom.planning.details

import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.uicommon.dom.{Button, Tooltip}
import com.leagueplans.uicommon.facades.floatingui.Placement
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.leagueplans.uicommon.wrappers.floatingui.FloatingConfig
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The steps above the focused step, outermost first. Each focuses its step when clicked. */
object Breadcrumbs {
  def apply(
    stepSignal: Signal[Step],
    forestSignal: Signal[Forest[Step.ID, Step]],
    focusController: FocusController,
    tooltip: Tooltip
  ): L.Element =
    L.navTag(
      L.cls(Styles.crumbs),
      L.aria.label("Superstep path"),
      L.children <-- Signal.combine(stepSignal.map(_.id), forestSignal).map((id, forest) =>
        forest.ancestors(id).reverse.flatMap(forest.get) match {
          case Nil => List(L.span("Top level"))
          case ancestors =>
            ancestors.zipWithIndex.flatMap((ancestor, index) =>
              Option.when(index > 0)(L.span(L.cls(Styles.separator), "›")).toList :+
                toCrumb(ancestor, focusController, tooltip)
            )
        }
      )
    )

  /** The tooltip shows the whole description, since long ones are cut short in the crumb */
  private def toCrumb(step: Step, focusController: FocusController, tooltip: Tooltip): L.Button =
    Button(_.handledAs(step.id) --> Observer(focusController.set)).amend(
      L.cls(Styles.crumb),
      step.description,
      tooltip.register(
        L.span(L.cls(Styles.tooltip), step.description),
        FloatingConfig.basicTooltip(Placement.bottom)
      )
    )

  @js.native @JSImport("/styles/planning/details/breadcrumbs.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val crumbs: String = js.native
    val crumb: String = js.native
    val separator: String = js.native
    val tooltip: String = js.native
  }
}
