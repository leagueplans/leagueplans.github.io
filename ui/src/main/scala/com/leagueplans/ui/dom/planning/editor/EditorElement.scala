package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{FormOpener, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.floatingui.Placement
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.utils.HasID
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.wrappers.floatingui.FloatingConfig
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EditorElement {
  def apply(
    cache: Cache,
    itemFuse: Fuse[Item],
    stepSignal: Signal[Step],
    warningsSignal: Signal[List[String]],
    forester: Forester[Step.ID, Step],
    tooltip: Tooltip,
    modal: Modal
  ): L.Div = {
    val effectRenderer = EffectRenderer(cache, tooltip)
    val requirementRenderer = RequirementRenderer(cache, tooltip)
    
    L.div(
      L.cls(Styles.editor),
      StepDescription(stepSignal, forester).amend(L.cls(Styles.description)),
      L.child <-- warningsSignal.map(toWarningIcon(_, tooltip)),
      L.div(
        L.cls(Styles.sections),
        L.child <-- toEffects(effectRenderer, stepSignal, forester),
        L.child <-- toRequirements(requirementRenderer, itemFuse, stepSignal, forester, modal)
      )
    )
  }

  @js.native @JSImport("/styles/planning/editor/editor.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val editor: String = js.native
    val description: String = js.native
    val warningIcon: String = js.native
    val warningTooltip: String = js.native
    val sections: String = js.native
    val section: String = js.native
  }

  private def toWarningIcon(warnings: List[String], tooltip: Tooltip): L.Node =
    if (warnings.isEmpty)
      L.emptyNode
    else
      L.div(
        L.cls(Styles.warningIcon),
        tooltip.register(
          L.div(L.cls(Styles.warningTooltip), warnings.map(L.p(_))),
          FloatingConfig.basicTooltip(Placement.right)
        ),
        FontAwesome.icon(FreeSolid.faTriangleExclamation)
      )

  private def toEffects(
    renderer: EffectRenderer,
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step]
  ): Signal[L.Div] =
    stepSignal.splitOne(_.id)((stepID, _, stepSignal) =>
      Section(
        title = "Effects",
        id = "effects",
        stepSignal.map(_.directEffects.underlying),
        Observer[List[Effect]](effectOrdering =>
          forester.update(stepID, _.deepCopy(directEffects = EffectList(effectOrdering)))
        ),
        renderer.render,
        None,
        Observer[Effect](deletedEffect =>
          forester.update(stepID, step => step.deepCopy(directEffects = step.directEffects - deletedEffect))
        )
      )(using HasID.identity).amend(L.cls(Styles.section))
    )

  private def toRequirements(
    renderer: RequirementRenderer,
    itemFuse: Fuse[Item],
    stepSignal: Signal[Step],
    forester: Forester[Step.ID, Step],
    modal: Modal
  ): Signal[L.Div] =
    stepSignal.splitOne(_.id)((stepID, _, stepSignal) =>
      Section(
        title = "Requirements",
        id = "requirements",
        stepSignal.map(_.requirements),
        Observer[List[Requirement]](requirementOrdering =>
          forester.update(stepID, _.deepCopy(requirements = requirementOrdering))
        ),
        renderer.render,
        Some(newRequirementObserver(itemFuse, stepID, modal, forester)),
        Observer[Requirement](deletedRequirement =>
          forester.update(stepID, step => step.deepCopy(requirements = step.requirements.filterNot(_ == deletedRequirement)))
        )
      )(using HasID.identity).amend(L.cls(Styles.section))
    )

  private def newRequirementObserver(
    itemFuse: Fuse[Item],
    stepID: Step.ID,
    modal: Modal,
    forester: Forester[Step.ID, Step]
  ): Observer[Any] =
    FormOpener(
      modal,
      NewRequirementForm(itemFuse),
      _.foreach(newRequirement =>
        forester.update(stepID, step => step.deepCopy(requirements = step.requirements :+ newRequirement))
      )
    ).toObserver
}
