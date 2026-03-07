package com.leagueplans.ui.dom.planning.editor.description

import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.editor.description.EditStepDescriptionForm
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.facades.fontawesome.freeregular.FreeRegular
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handledWith
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StepDescription {
  def apply(
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    tooltip: Tooltip,
    modal: Modal
  ): L.Div =
    L.div(
      L.cls(Styles.content),
      Button(
        _.handledWith(_.sample(stepSignal)) -->
          Observer(step =>
            EditStepDescriptionForm.open(step, forester, modal)
          )
      ).amend(
        L.cls(Styles.editButton),
        FontAwesome.icon(FreeRegular.faPenToSquare),
        IconButtonModifiers(
          tooltipContents = "Edit description",
          screenReaderDescription = "edit description",
          tooltip,
          tooltipPlacement = Placement.top
        )
      ),
      L.p(
        L.cls(Styles.description),
        L.text <-- stepSignal.map(_.description)
      )
    )

  @js.native @JSImport("/styles/planning/editor/description/stepDescription.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val content: String = js.native
    val editButton: String = js.native
    val description: String = js.native
  }
}