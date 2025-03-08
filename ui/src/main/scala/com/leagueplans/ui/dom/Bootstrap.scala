package com.leagueplans.ui.dom

import com.leagueplans.common.model.{Item, LeagueTask}
import com.leagueplans.ui.dom.common.{ContextMenu, Modal, ToastHub}
import com.leagueplans.ui.dom.landing.LandingPage
import com.leagueplans.ui.dom.planning.PlanningPage
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.diary.DiaryTask
import com.leagueplans.ui.model.player.{Cache, Quest}
import com.leagueplans.ui.storage.client.PlanSubscription
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import io.circe.Decoder
import io.circe.scalajs.decodeJs

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Bootstrap {
  def apply(): L.Div = {
    val (contextMenu, contextMenuController) = ContextMenu()
    val (modalElement, modalController) = Modal()
    val (toastHub, toastPublisher) = ToastHub()

    L.div(
      L.idAttr("bootstrap"),
      contextMenu,
      modalElement,
      toastHub,
      L.child <-- toPageSignal(contextMenuController, modalController, toastPublisher),
      EventStream.unit(emitOnce = true).delay(2000) --> (_ =>
        toastPublisher.publish(feedbackToast())
      )
    )
  }

  private def toPageSignal(
    contextMenuController: ContextMenu.Controller,
    modal: Modal,
    toastPublisher: ToastHub.Publisher
  ): Signal[L.Div] = {
    val planBus = EventBus[(Plan, PlanSubscription)]()
    planBus
      .events
      .combineWith(loadCache(toastPublisher))
      .map((plan, subscription, cache) =>
        PlanningPage(
          plan,
          subscription,
          cache,
          contextMenuController,
          modal,
          toastPublisher
        )
      )
      .toSignal(initial = LandingPage(planBus.writer, toastPublisher, modal))
  }

  private def loadCache(toastPublisher: ToastHub.Publisher): EventStream[Cache] =
    EventStream.fromJsPromise(
      js.dynamicImport(
        Cache(
          decode[Set[Item]](itemsJson, toastPublisher, "Failed to decode item data"),
          decode[Set[Quest]](questsJson, toastPublisher, "Failed to decode quest data"),
          decode[Set[DiaryTask]](diaryTasksJson, toastPublisher, "Failed to decode diary task data"),
          decode[Set[LeagueTask]](leagueTasksJson, toastPublisher, "Failed to decode league task data")
        )
      )
    )

  private def decode[T : Decoder](
    json: js.Object,
    toastPublisher: ToastHub.Publisher,
    toastMessage: String
  ): T =
    decodeJs[T](json) match {
      case Right(value) => value
      case Left(error) =>
        toastPublisher.publish(ToastHub.Type.Error, Duration.Inf, toastMessage)
        throw error
    }

  @js.native @JSImport("/data/items.json", JSImport.Default)
  private def itemsJson: js.Object = js.native

  @js.native @JSImport("/data/quests.json", JSImport.Default)
  private def questsJson: js.Object = js.native

  @js.native @JSImport("/data/diaryTasks.json", JSImport.Default)
  private def diaryTasksJson: js.Object = js.native

  @js.native @JSImport("/data/leagueTasks.json", JSImport.Default)
  private def leagueTasksJson: js.Object = js.native

  private def feedbackToast(): ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Info,
      10.seconds,
      L.span("To offer feedback, contact @Granarder on discord.")
    )
}
