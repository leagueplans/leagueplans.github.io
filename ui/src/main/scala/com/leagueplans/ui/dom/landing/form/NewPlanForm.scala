package com.leagueplans.ui.dom.landing.form

import cats.data.NonEmptyList
import com.leagueplans.codec.decoding.DecodingFailure
import com.leagueplans.ui.dom.common.form.{Form, Select, TextInput}
import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.storage.ExportedPlanDecoder
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.ui.storage.model.errors.FileSystemError
import com.leagueplans.ui.storage.model.{PlanExport, PlanID, PlanMetadata}
import com.leagueplans.ui.utils.airstream.EventStreamOps.andThen
import com.leagueplans.ui.utils.airstream.PromiseLikeOps.onComplete
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewPlanForm {
  def apply(
    storage: StorageClient,
    planObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher
  ): L.FormElement = {
    val (nameInput, nameLabel, nameSignal) = createNameInput()
    val (modeSelect, modeLabel, modeSignal) = createModeSelect()
    val (importInput, importLabel, importSignal) = createImportInput()

    val (form, submitButton) = createForm(() => 
      onSubmit(
        storage,
        planObserver, 
        toastPublisher,
        Signal.combine(nameSignal, modeSignal, importSignal)
      )
    )

    form.amend(
      L.cls(Styles.form),
      nameLabel.amend(L.cls(Styles.label), "Name:"),
      nameInput.amend(L.cls(Styles.input)),
      modeLabel.amend(L.cls(Styles.label), "Game mode:"),
      modeSelect.amend(L.cls(Styles.input)),
      L.div(
        InfoIcon().amend(L.svg.cls(Styles.infoIcon)),
        importLabel.amend(L.cls(Styles.label), "Initial data:"),
        Tooltip(L.p(
          "If you have a save file for an existing plan, then you can import it here. Otherwise, you can ignore this."
        ))
      ),
      importInput.amend(L.cls(Styles.input)),
      L.child <-- submitButton.map(_.amend(L.cls(Styles.submit)))
    )
  }

  @js.native @JSImport("/styles/landing/form/newPlanForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native
    val label: String = js.native
    val input: String = js.native
    val submit: String = js.native

    val infoIcon: String = js.native
  }

  private def createNameInput(): (L.Input, L.Label, Signal[String]) = {
    val (baseInput, nameLabel, nameSignal) = TextInput(
      TextInput.Type.Text,
      id = "new-plan-name-entry",
      initial = ""
    )

    val nameInput = baseInput.amend(L.required(true))
    (nameInput, nameLabel, nameSignal)
  }

  private def createModeSelect(): (L.Select, L.Label, Signal[Mode]) =
    Select(
      id = "new-plan-mode-select",
      NonEmptyList.fromListUnsafe(Mode.all).map(mode =>
        Select.Opt(mode, mode.name)
      )
    )

  private def createImportInput(): (L.Input, L.Label, Signal[Option[PlanExport]]) =
    GzipFileInput[PlanExport](id = "import-existing-plan-input")
    
  private def createForm[T](
    onSubmit: () => EventStream[?]
  ): (L.FormElement, Signal[L.HtmlElement]) = {
    val isBusy = Var(false)
    val submissionObserver =
      Observer.combine[Unit](
        isBusy.writer.contramap(_ => true),
        Observer(_ => onSubmit().onComplete(_ => isBusy.set(false)))
      )

    val (emptyForm, submitButton, formSubmissions) = Form()
    val formWithTracking = emptyForm.amend(formSubmissions --> submissionObserver)
    val submitOrLoading = isBusy.signal.splitOne(identity) {
      case (true, _, _) => L.div(LoadingIcon())
      case (false, _, _) => submitButton.amend(L.value("Create plan"))
    }
    
    (formWithTracking, submitOrLoading)
  }

  private def onSubmit(
    storage: StorageClient,
    planObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher,
    inputSignal: Signal[(String, Mode, Option[PlanExport])]
  ): EventStream[Unit] =
    EventStream
      .fromValue(())
      .sample(inputSignal)
      .map(toPlan)
      .andThen[DecodingFailure | FileSystemError, PlanID]((metadata, plan) =>
        storage.create(metadata, plan).changes.collectSome
      )
      .andThen(planID => storage.subscribe(planID).changes.collectSome)
      .map { result =>
        result match {
          case Right(plan) => 
            planObserver.onNext(plan)
            
          case Left(error: DecodingFailure) => 
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              s"Failed to decode imported data. Cause: [${error.getMessage}]"
            )
            
          case Left(error: FileSystemError) =>
            toastPublisher.publish(
              ToastHub.Type.Warning,
              15.seconds,
              s"Failed to create plan. Cause: [${error.message}]"
            )
        }
        ()
      }
    
  private def toPlan(
    name: String, 
    mode: Mode, 
    maybeImport: Option[PlanExport]
  ): Either[DecodingFailure, (PlanMetadata, Plan)] =
    maybeImport match {
      case None =>
        val step = Step(name)
        val steps = Forest.from(Map(step.id -> step), Map(step.id -> List.empty))
        Right((PlanMetadata(name), Plan(steps, mode.settings)))

      case Some(planImport) =>
        ExportedPlanDecoder
          .decode(planImport)
          .map((metadata, plan) => (metadata.copy(name = name), plan.copy(settings = mode.settings)))
    }
}
