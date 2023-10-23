package ddm.ui.dom.plans

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.laminar.api.{L, enrichSource, seqToModifier, textToTextNode, optionToModifier}
import ddm.ui.PlanStorage
import ddm.ui.PlanStorage.Result
import ddm.ui.dom.common.ToastHub
import ddm.ui.dom.common.ToastHub.Toast
import ddm.ui.model.plan.Plan
import org.scalajs.dom.console

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object LandingPage {
  def apply(
    planStorage: PlanStorage,
    planObserver: Observer[Plan.Named],
    toastBus: WriteBus[Toast],
    modalBus: WriteBus[Option[L.Element]]
  ): L.Div = {
    val loadObserver = Observer[PlanStorage.Result] {
      case Result.Success(plan) =>
        planObserver.onNext(plan)
      case Result.Failure(error) =>
        console.error(message = s"Failed to load plan: [${error.getMessage}]")
        toastBus.onNext(toastParsingFailure())
      case Result.None =>
        console.warn(message = "Tried to load a plan that did not exist")
        toastBus.onNext(toastPlanDoesNotExist())
    }

    val (newPlanForm, newPlanStream) = NewPlanForm(planStorage.plansSignal)

    L.div(
      L.cls(Styles.page),
      L.div(
        L.cls(Styles.content),
        L.p(L.cls(Styles.intro), "Start a new plan from scratch"),
        newPlanForm,
        Option.when(planStorage.plansSignal.now().nonEmpty)(
          List(
            L.p(L.cls(Styles.intro), "Or select an existing plan"),
            PlansList(planStorage, loadObserver, modalBus),
          )
        )
      ),
      newPlanStream --> planObserver
    )
  }

  @js.native @JSImport("/styles/plans/landingPage.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val page: String = js.native
    val intro: String = js.native
    val content: String = js.native
  }

  private def toastParsingFailure(): Toast =
    Toast(
      ToastHub.Type.Error,
      Duration.Inf,
      L.span(
        "Could not parse the plan. This probably means the plan was built with an incompatible" +
          " version of this app."
      )
    )

  private def toastPlanDoesNotExist(): Toast =
    Toast(ToastHub.Type.Warning, 30.seconds, L.span("Unable to load plan."))
}
