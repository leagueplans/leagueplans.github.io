package ddm.ui.dom

import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.WriteBus
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import ddm.common.model.{Item, LeagueTask}
import ddm.ui.PlanStorage
import ddm.ui.dom.common.{ContextMenu, Modal, ToastHub}
import ddm.ui.dom.plans.LandingPage
import ddm.ui.model.plan.SavedState
import ddm.ui.model.player.diary.DiaryTask
import ddm.ui.model.player.{Cache, Quest}
import io.circe.Decoder
import io.circe.scalajs.decodeJs

import scala.concurrent.duration.{Duration, DurationInt}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Bootstrap {
  def apply(storage: PlanStorage): L.Div = {
    val (contextMenu, contextMenuController) = ContextMenu()
    val (modal, modalBus) = Modal()
    val (toastHub, toastBus) = ToastHub()

    val pageVar = Var[L.Node](L.emptyNode)
    val loadedPlanObserver =
      pageVar.writer.contramap[SavedState.Named](
        PlanningPage(storage, _, loadCache(toastBus), contextMenuController, modalBus, toastBus)
      )
    pageVar.writer.onNext(LandingPage(storage, loadedPlanObserver, toastBus, modalBus))

    L.div(
      L.idAttr("bootstrap"),
      contextMenu,
      modal,
      toastHub,
      L.child <-- pageVar,
      EventStream.unit(emitOnce = true).delay(2000) --> Observer[Unit](_ =>
        toastBus.onNext(feedbackToast())
      ),
      EventStream.unit(emitOnce = true).delay(2000) --> Observer[Unit](_ =>
        toastBus.onNext(underDevelopmentToast())
      )
    )
  }

  @js.native @JSImport("/data/items.json", JSImport.Default)
  private def itemsJson: js.Object = js.native

  @js.native @JSImport("/data/quests.json", JSImport.Default)
  private def questsJson: js.Object = js.native

  @js.native @JSImport("/data/diaryTasks.json", JSImport.Default)
  private def diaryTasksJson: js.Object = js.native

  @js.native @JSImport("/data/leagueTasks.json", JSImport.Default)
  private def leagueTasksJson: js.Object = js.native

  private def loadCache(toastBus: WriteBus[ToastHub.Toast]): Cache =
    Cache(
      decode[Set[Item]](itemsJson, toastBus, "Failed to decode item data"),
      decode[Set[Quest]](questsJson, toastBus, "Failed to decode quest data"),
      decode[Set[DiaryTask]](diaryTasksJson, toastBus, "Failed to decode diary task data"),
      decode[Set[LeagueTask]](leagueTasksJson, toastBus, "Failed to decode league task data")
    )

  private def decode[T : Decoder](
    json: js.Object,
    toastBus: WriteBus[ToastHub.Toast],
    toastMessage: String
  ): T =
    decodeJs[T](json) match {
      case Right(value) => value
      case Left(error) =>
        toastBus.onNext(ToastHub.Toast(ToastHub.Type.Error, Duration.Inf, L.span(toastMessage)))
        throw error
    }

  private def feedbackToast(): ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Info,
      10.seconds,
      L.span("To offer feedback, contact @Granarder on discord.")
    )

  private def underDevelopmentToast(): ToastHub.Toast =
    ToastHub.Toast(
      ToastHub.Type.Warning,
      30.seconds,
      L.span("This site is under active development. It's likely that breaking changes will be made" +
        " once the league ends, which will cause existing plans to fail to render.")
    )
}
