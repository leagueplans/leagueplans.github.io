package ddm.ui.dom.editor

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.{Forester, FormOpener}
import ddm.ui.model.plan.{Effect, EffectList, Step}
import ddm.ui.model.player.Quest
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.Div

import java.util.UUID
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object EditorElement {
  def apply(
    quests: Fuse[Quest],
    dataSignal: Signal[(Step, List[Step])],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    modalBus: WriteBus[Option[L.Element]],
    showEffect: Effect => L.HtmlElement
  ): ReactiveHtmlElement[Div] = {
    val stepSignal = dataSignal.map { case (step, _) => step }
    val subStepsSignal = dataSignal.map { case (_, subSteps) => subSteps }

    L.div(
      L.cls(Styles.editor),
      StepDescription(stepSignal, stepUpdater),
      L.div(
        L.cls(Styles.sections),
        L.child <-- toSubSteps(stepSignal, subStepsSignal, stepUpdater, modalBus),
        L.child <-- toEffects(quests, stepSignal, stepUpdater, modalBus, showEffect)
      )
    )
  }

  @js.native @JSImport("/styles/editor/editor.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val editor: String = js.native
    val sections: String = js.native
    val section: String = js.native
    val text: String = js.native
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
          L.cls(Styles.text),
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
    quests: Fuse[Quest],
    stepSignal: Signal[Step],
    stepUpdater: Observer[Forester[UUID, Step] => Unit],
    modalBus: WriteBus[Option[L.Element]],
    showEffect: Effect => L.HtmlElement
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
        showEffect,
        newEffectObserver(quests, stepID, modalBus, stepUpdater),
        stepUpdater.contramap[Effect](deletedEffect => forester =>
          forester.update(stepID, step => step.copy(directEffects = step.directEffects - deletedEffect))
        )
      ).amend(L.cls(Styles.section))
    }

  private def newEffectObserver(
    quests: Fuse[Quest],
    stepID: UUID,
    modalBus: WriteBus[Option[L.Element]],
    stepUpdater: Observer[Forester[UUID, Step] => Unit]
  ): Observer[FormOpener.Command] = {
    val (form, formSubmissions) = NewEffectForm(quests)
    FormOpener(
      modalBus,
      stepUpdater.contracollect[Option[Effect]] { case Some(newEffect) => forester =>
        forester.update(stepID, step => step.copy(directEffects = step.directEffects + newEffect))
      },
      () => (form, formSubmissions)
    )
  }
}
