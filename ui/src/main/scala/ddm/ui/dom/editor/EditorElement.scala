package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, seqToModifier, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.common.model.Item
import ddm.ui.dom.common.{Forester, FormOpener, Tooltip}
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.{Effect, EffectList, Requirement, Step}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.model.validation.StepValidator
import ddm.ui.utils.laminar.LaminarOps.RichL
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.Div

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EditorElement {
  def apply(
    cache: Cache,
    itemFuse: Fuse[Item],
    dataSignal: Signal[(Step, List[Step], Player)],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    modalBus: WriteBus[Option[L.Element]]
  ): ReactiveHtmlElement[Div] = {
    val stepSignal = dataSignal.map { case (step, _, _) => step }
    val subStepsSignal = dataSignal.map { case (_, subSteps, _) => subSteps }
    val playerSignal = dataSignal.map { case (_, _, player) => player }

    L.div(
      L.cls(Styles.editor),
      StepDescription(stepSignal, stepUpdater).amend(L.cls(Styles.description)),
      L.child <-- toWarnings(cache, playerSignal, stepSignal),
      L.div(
        L.cls(Styles.sections),
        L.child <-- toSubSteps(stepSignal, subStepsSignal, stepUpdater, modalBus),
        L.child <-- toEffects(cache, stepSignal, stepUpdater, modalBus),
        L.child <-- toRequirements(cache, itemFuse, stepSignal, stepUpdater, modalBus)
      )
    )
  }

  @js.native @JSImport("/styles/editor/editor.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val editor: String = js.native
    val description: String = js.native
    val warningIcon: String = js.native
    val sections: String = js.native
    val section: String = js.native
    val tooltip: String = js.native
    val subStepDescription: String = js.native
  }

  private def toWarnings(
    cache: Cache,
    playerSignal: Signal[Player],
    stepSignal: Signal[Step]
  ): Signal[L.Child] =
    Signal
      .combine(playerSignal, stepSignal)
      .map { case (player, step) =>
        val errors = StepValidator.validate(step)(player, cache)

        if (errors.isEmpty)
          L.emptyNode
        else
          L.div(
            L.cls(Styles.warningIcon),
            Tooltip(L.div(
              L.cls(Styles.tooltip),
              errors.map(L.p(_))
            )),
            L.icon(FreeSolid.faTriangleExclamation)
          )
      }

  private def toSubSteps(
    stepSignal: Signal[Step],
    subStepsSignal: Signal[List[Step]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    modalBus: WriteBus[Option[L.Element]],
  ): Signal[ReactiveHtmlElement[Div]] =
    stepSignal.splitOne(_.id) { case (stepID, _, _) =>
      Section[UUID, Step](
        title = "Steps",
        id = "substeps",
        subStepsSignal,
        stepUpdater.contramap[List[Step]](reordering => forester =>
          forester.reorder(reordering.map(_.id))
        ),
        _.id,
        subStep => L.p(
          L.cls(Styles.subStepDescription),
          subStep.description
        ),
        newSubStepObserver(stepID, modalBus, stepUpdater),
        stepUpdater.contramap[Step](deletedStep => forester =>
          // Bit messy, but it works for now
          DeletionConfirmer(modalBus, Observer[Unit](_ => forester.remove(deletedStep.id))).onNext(())
        )
      ).amend(L.cls(Styles.section))
    }

  private def newSubStepObserver(
    parentID: UUID,
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = NewStepForm()
    FormOpener(
      modalBus,
      stepUpdater.contracollect[Option[Step]] { case Some(newStep) => forester =>
        forester.add(newStep, parentID)
      },
      () => (form, formSubmissions)
    )
  }

  private def toEffects(
    cache: Cache,
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    modalBus: WriteBus[Option[L.Element]]
  ): Signal[ReactiveHtmlElement[Div]] =
    stepSignal.splitOne(_.id) { case (stepID, _, stepSignal) =>
      Section[Effect, Effect](
        title = "Effects",
        id = "effects",
        stepSignal.map(_.directEffects.underlying),
        stepUpdater.contramap[List[Effect]](effectOrdering => forester =>
          forester.update(stepID, _.copy(directEffects = EffectList(effectOrdering)))
        ),
        identity,
        DescribedEffect(_, cache),
        newEffectObserver(stepID, modalBus, stepUpdater),
        stepUpdater.contramap[Effect](deletedEffect => forester =>
          forester.update(stepID, step => step.copy(directEffects = step.directEffects - deletedEffect))
        )
      ).amend(L.cls(Styles.section))
    }

  private def newEffectObserver(
    stepID: UUID,
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = NewEffectForm()
    FormOpener(
      modalBus,
      stepUpdater.contracollect[Option[Effect]] { case Some(newEffect) => forester =>
        forester.update(stepID, step => step.copy(directEffects = step.directEffects + newEffect))
      },
      () => (form, formSubmissions)
    )
  }

  private def toRequirements(
    cache: Cache,
    itemFuse: Fuse[Item],
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    modalBus: WriteBus[Option[L.Element]]
  ): Signal[ReactiveHtmlElement[Div]] =
    stepSignal.splitOne(_.id) { case (stepID, _, stepSignal) =>
      Section[Requirement, Requirement](
        title = "Requirements",
        id = "requirements",
        stepSignal.map(_.requirements),
        stepUpdater.contramap[List[Requirement]](requirementOrdering => forester =>
          forester.update(stepID, _.copy(requirements = requirementOrdering))
        ),
        identity,
        DescribedRequirement(_, cache),
        newRequirementObserver(itemFuse, stepID, modalBus, stepUpdater),
        stepUpdater.contramap[Requirement](deletedRequirement => forester =>
          forester.update(stepID, step => step.copy(requirements = step.requirements.filterNot(_ == deletedRequirement)))
        )
      ).amend(L.cls(Styles.section))
    }

  private def newRequirementObserver(
    itemFuse: Fuse[Item],
    stepID: UUID,
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = NewRequirementForm(itemFuse)
    FormOpener(
      modalBus,
      stepUpdater.contracollect[Option[Requirement]] { case Some(newRequirement) => forester =>
        forester.update(stepID, step => step.copy(requirements = step.requirements :+ newRequirement))
      },
      () => (form, formSubmissions)
    )
  }
}
