package com.leagueplans.ui.dom.planning.plan

import com.leagueplans.ui.dom.common.{Button, DeletionConfirmer, FormOpener, Modal}
import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.utils.laminar.EventProcessorOps.{handled, handledWith}
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

//TODO Outline styling for modal buttons
//TODO Delete step confirmation should show a preview of the steps to be deleted
object PlanHeader {
  def apply(
    planName: String,
    forestSignal: Signal[Forest[Step.ID, Step]],
    focusController: FocusedStep.Controller,
    modalController: Modal.Controller,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): L.Div = {
    val addStepObserver = createAddStepObserver(focusController.signal, modalController, stepUpdater)
    val deleteStepObserver = createDeleteStepObserver(focusController, modalController, stepUpdater)
    val stepSignal =
      Signal
        .combine(focusController.signal, forestSignal)
        .map((maybeStepID, forest) => maybeStepID.flatMap(forest.nodes.get))

    L.div(
      L.cls(Styles.header),
      L.img(L.cls(Styles.icon), L.src(icon), L.alt("Plan section icon")),
      L.span(L.cls(Styles.name), planName),
      toAddStepButton(addStepObserver),
      toDeleteStepButton(stepSignal, deleteStepObserver)
    )
  }

  @js.native @JSImport("/assets/images/favicon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/planning/plan/planHeader.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val header: String = js.native
    val icon: String = js.native
    val name: String = js.native
    val addStepButton: String = js.native
    val deleteStepButton: String = js.native
    val buttonText: String = js.native
  }

  private def toAddStepButton(observer: Observer[Unit]): L.Button =
    Button(_.handled --> observer).amend(
      L.cls(Styles.addStepButton),
      L.span(L.cls(Styles.buttonText), "Add step")
    )

  private def createAddStepObserver(
    focusedStep: Signal[Option[Step.ID]],
    modalController: Modal.Controller,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): Observer[FormOpener.Command] =
    FormOpener(
      modalController,
      stepUpdater.contracollect[(Option[Step], Option[Step.ID])] {
        case (Some(child), Some(parent)) => _.add(child, parent)
        case (Some(child), None) => _.add(child)
      },
      () => {
        val (form, submissions) = NewStepForm()
        (form, submissions.withCurrentValueOf(focusedStep))
      }
    )

  //TODO It'd be nice to have a tooltip here explaining why when the button is disabled.
  // This'll need to wait for a rework of tooltips though, as currently it isn't possible
  // to optionally define a tooltip without splitting the button.
  private def toDeleteStepButton(
    step: Signal[Option[Step]],
    observer: Observer[Step]
  ): L.Button =
    Button(_.handledWith(_.sample(step).collectSome) --> observer).amend(
      L.cls(Styles.deleteStepButton),
      L.disabled <-- step.map(_.isEmpty),
      L.span(L.cls(Styles.buttonText), "Delete step"),
    )

  private def createDeleteStepObserver(
    focusController: FocusedStep.Controller,
    modalController: Modal.Controller,
    stepUpdater: Observer[Forester[Step.ID, Step] => Unit]
  ): Observer[Step] =
    stepUpdater.contramap[Step](step => forester =>
      DeletionConfirmer(
        s"\"${step.details.description}\" and all its nested substeps will be permanently deleted." +
          s" This cannot be undone.",
        "Delete step",
        modalController,
        Observer { _ =>
          focusController.next(ignoreChildren = true)
          forester.remove(step.id)
        }
      ).onNext(())
    )
}
