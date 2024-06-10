package ddm.ui.dom.landing.menu

import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.laminar.api.{L, textToTextNode}
import ddm.ui.dom.common.ToastHub
import ddm.ui.model.plan.Plan
import ddm.ui.storage.client.{PlanSubscription, StorageClient}
import ddm.ui.storage.model.PlanID

import scala.concurrent.duration.DurationInt

object LoadButton {
  def apply(
    id: PlanID,
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher
  ): L.Button =
    AsyncButton(
      "Load",
      () => onClick(id, storage, selectionObserver, toastPublisher)
    )

  private def onClick(
    id: PlanID,
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher
  ): EventStream[Unit] =
    storage.subscribe(id).changes.collectSome.map { result =>
      result match {
        case Right(plan) => selectionObserver.onNext(plan)
        case Left(error) => 
          toastPublisher.publish(
            ToastHub.Type.Warning,
            15.seconds,
            s"Failed to load plan. Cause: [${error.message}]"
          )
      }
      ()
    }
}
