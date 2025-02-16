package com.leagueplans.ui.dom.landing.menu

import com.leagueplans.ui.dom.common.{Button, ToastHub}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.client.{PlanSubscription, StorageClient}
import com.leagueplans.ui.storage.model.PlanID
import com.leagueplans.ui.utils.laminar.LaminarOps.handled
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, textToTextNode}

import scala.concurrent.duration.DurationInt

object LoadButton {
  def apply(
    id: PlanID,
    storage: StorageClient,
    selectionObserver: Observer[(Plan, PlanSubscription)],
    toastPublisher: ToastHub.Publisher
  ): L.Button = {
    val clickStream = EventBus[Unit]()

    Button(_.handled --> clickStream.writer).amend(
      AsyncButtonModifiers(
        "Load",
        clickStream.events.flatMapWithStatus(
          onClick(id, storage, selectionObserver, toastPublisher)
        )
      )
    )
  }

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
