package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.editor.{EffectRenderer, NewRequirementForm, RequirementRenderer, Section}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.dom.planning.plan.FocusController
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.projection.model.StepError
import com.leagueplans.uicommon.dom.{FormOpener, Modal, Tooltip}
import com.leagueplans.uicommon.utils.HasID
import com.leagueplans.uicommon.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** Everything about the focused step: where it sits in the plan, its description, timings,
  * problems, effects and requirements */
object StepDetails {
  def apply(
    cache: Cache,
    itemFuse: Fuse[Item],
    stepSignal: Signal[Step],
    errorsSignal: Signal[List[StepError]],
    forester: Forester[Step.ID, Step],
    focusController: FocusController,
    timeKeeper: TimeKeeper,
    descriptionFocusRequests: EventStream[Unit],
    tooltip: Tooltip,
    modal: Modal
  ): L.Div = {
    val effectRenderer = EffectRenderer(cache, tooltip)
    val requirementRenderer = RequirementRenderer(cache, tooltip)

    L.div(
      L.cls(Styles.details),
      L.div(
        L.cls(Styles.header),
        Breadcrumbs(stepSignal, forester.signal, focusController, tooltip),
        DescriptionField(stepSignal, forester, descriptionFocusRequests),
        TimingRows(stepSignal, forester, timeKeeper, tooltip)
      ),
      ProblemList(errorsSignal, EffectText(cache)),
      L.child <-- toEffects(effectRenderer, stepSignal, forester),
      L.child <-- toRequirements(requirementRenderer, itemFuse, stepSignal, forester, modal)
    )
  }

  @js.native @JSImport("/styles/planning/details/stepDetails.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val details: String = js.native
    val header: String = js.native
    val section: String = js.native
  }

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
