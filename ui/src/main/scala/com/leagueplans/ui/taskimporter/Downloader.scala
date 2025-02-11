package com.leagueplans.ui.taskimporter

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.Button
import com.leagueplans.ui.utils.laminar.LaminarOps.ifUnhandledF
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import io.circe.syntax.EncoderOps
import org.scalajs.dom.{Blob, BlobPropertyBag, MouseEvent, URL}

import scala.scalajs.js.JSConverters.JSRichIterable

object Downloader {
  def apply(stateSignal: Signal[StateTracker.State]): L.Div =
    L.div(
      L.child <-- stateSignal.map(state => s"${state.remainingNewTasks.size} tasks left to resolve"),
      updatedTasksButton(stateSignal),
      remainingTasksButton(stateSignal)
    )

  private def updatedTasksButton(stateSignal: Signal[StateTracker.State]): L.Button =
    Button(Observer[(MouseEvent, StateTracker.State)] { (event, state) =>
      event.preventDefault()
      val tasks = state.processedTasks ++ state.remainingExistingTasks.values
      triggerDownload(tasks, "updated-tasks")
    })(_.ifUnhandledF(_.withCurrentValueOf(stateSignal))).amend("Download updated tasks")

  private def remainingTasksButton(stateSignal: Signal[StateTracker.State]): L.Button =
    Button(Observer[(MouseEvent, StateTracker.State)] { (event, state) =>
      event.preventDefault()
      val tasks = state.remainingNewTasks.values.toList
      triggerDownload(tasks, "remaining-tasks")
    })(_.ifUnhandledF(_.withCurrentValueOf(stateSignal))).amend("Download remaining tasks")

  private def triggerDownload(data: List[LeagueTask], name: String): Unit = {
    val url = createDownloadURL(data)
    L.a(L.href(url), L.download(s"$name.json")).ref.click()
    URL.revokeObjectURL(url)
  }

  private def createDownloadURL(data: List[LeagueTask]): String = {
    val blob = Blob(
      List(data.sorted.asJson.noSpaces).toJSIterable,
      new BlobPropertyBag { `type` = "application/json" }
    )
    URL.createObjectURL(blob)
  }
}
