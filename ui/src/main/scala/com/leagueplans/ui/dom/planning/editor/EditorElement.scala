package com.leagueplans.ui.dom.planning.editor

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.common.{FormOpener, Modal, Tooltip}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.model.plan.{Effect, EffectList, Requirement, Step}
import com.leagueplans.ui.model.player.Cache
import com.leagueplans.ui.utils.HasID
import com.leagueplans.ui.utils.laminar.FontAwesome
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
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    modalController: Modal.Controller
  ): L.Div =
    L.div(
      L.cls(Styles.editor),
      StepDescription(stepSignal, stepUpdater).amend(L.cls(Styles.description)),
      L.child <-- warningsSignal.map(toWarningIcon),
      L.div(
        L.cls(Styles.sections),
        L.child <-- toEffects(cache, stepSignal, stepUpdater),
        L.child <-- toRequirements(cache, itemFuse, stepSignal, stepUpdater, modalController)
      )
    )

  @js.native @JSImport("/styles/planning/editor/editor.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val editor: String = js.native
    val description: String = js.native
    val warningIcon: String = js.native
    val sections: String = js.native
    val section: String = js.native
  }

  private def toWarningIcon(warnings: List[String]): L.Node =
    if (warnings.isEmpty)
      L.emptyNode
    else
      L.div(
        L.cls(Styles.warningIcon),
        Tooltip(L.div(warnings.map(L.p(_)))),
        FontAwesome.icon(FreeSolid.faTriangleExclamation)
      )

  private def toEffects(
    cache: Cache,
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): Signal[L.Div] =
    stepSignal.splitOne(_.id)((stepID, _, stepSignal) =>
      Section(
        title = "Effects",
        id = "effects",
        stepSignal.map(_.directEffects.underlying),
        stepUpdater.contramap[List[Effect]](effectOrdering => forester =>
          forester.update(stepID, _.deepCopy(directEffects = EffectList(effectOrdering)))
        ),
        DescribedEffect(_, cache),
        None,
        stepUpdater.contramap[Effect](deletedEffect => forester =>
          forester.update(stepID, step => step.deepCopy(directEffects = step.directEffects - deletedEffect))
        )
      )(using HasID.identity).amend(L.cls(Styles.section))
    )

  private def toRequirements(
    cache: Cache,
    itemFuse: Fuse[Item],
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit],
    modalController: Modal.Controller
  ): Signal[L.Div] =
    stepSignal.splitOne(_.id)((stepID, _, stepSignal) =>
      Section(
        title = "Requirements",
        id = "requirements",
        stepSignal.map(_.requirements),
        stepUpdater.contramap[List[Requirement]](requirementOrdering => forester =>
          forester.update(stepID, _.deepCopy(requirements = requirementOrdering))
        ),
        DescribedRequirement(_, cache),
        Some(newRequirementObserver(itemFuse, stepID, modalController, stepUpdater)),
        stepUpdater.contramap[Requirement](deletedRequirement => forester =>
          forester.update(stepID, step => step.deepCopy(requirements = step.requirements.filterNot(_ == deletedRequirement)))
        )
      )(using HasID.identity).amend(L.cls(Styles.section))
    )

  private def newRequirementObserver(
    itemFuse: Fuse[Item],
    stepID: Step.ID,
    modalController: Modal.Controller,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalController,
      stepUpdater.contracollect[Option[Requirement]] { case Some(newRequirement) => forester =>
        forester.update(stepID, step => step.deepCopy(requirements = step.requirements :+ newRequirement))
      },
      () => NewRequirementForm(itemFuse)
    )
}
