package com.leagueplans.ui.taskimporter

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.Button
import com.leagueplans.ui.utils.laminar.LaminarOps.handledAs
import com.raquo.airstream.core.{EventStream, Observer, Signal}
import com.raquo.laminar.api.{L, textToTextNode}
import io.circe.Printer
import io.circe.syntax.EncoderOps

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DecisionMaker {
  enum Decision {
    case New(task: LeagueTask)
    case Merge(existingTask: LeagueTask, newTask: LeagueTask, mergedTask: LeagueTask)
  }

  def apply(
    existingTaskStream: EventStream[LeagueTask],
    newTaskSignal: Signal[LeagueTask],
    observer: Observer[Decision]
  ): L.Div =
    L.div(
      L.cls(Styles.decisionMaker),
      L.children <--
        existingTaskStream
          .withCurrentValueOf(newTaskSignal)
          .map((existingTask, newTask) =>
            List(
              mergeChoice(
                "Merge (prefer existing name/description)",
                existingTask,
                newTask,
                TaskMerger.mergePrioExisting(existingTask, newTask),
                observer
              ),
              mergeChoice(
                "Merge (prefer new name/description)",
                existingTask,
                newTask,
                TaskMerger.mergePrioNew(existingTask, newTask),
                observer
              ),
            )
          ),
      L.child <-- newTaskSignal.map(newButton(_, observer))
    )

  @js.native @JSImport("/styles/taskimporter/decisionMaker.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val decisionMaker: String = js.native
    val preview: String = js.native
    val merge: String = js.native
    val mergeButton: String = js.native
    val `new`: String = js.native
  }

  private def mergeChoice(
    descriptor: String,
    existingTask: LeagueTask,
    newTask: LeagueTask,
    mergedTask: LeagueTask,
    observer: Observer[Decision]
  ): L.Div =
    L.div(
      L.cls(Styles.merge),
      mergeButton(descriptor, existingTask, newTask, mergedTask, observer),
      preview(mergedTask)
    )

  private def preview(mergedTask: LeagueTask): L.Div =
    L.div(L.cls(Styles.preview), printer.print(mergedTask.asJson))

  private val printer = Printer.spaces2.copy(dropNullValues = true, lrbracketsEmpty = "")

  private def mergeButton(
    descriptor: String,
    existingTask: LeagueTask,
    newTask: LeagueTask,
    mergedTask: LeagueTask,
    observer: Observer[Decision]
  ): L.Button =
    Button(_.handledAs(Decision.Merge(existingTask, newTask, mergedTask)) --> observer).amend(
      L.cls(Styles.mergeButton),
      descriptor
    )

  private def newButton(newTask: LeagueTask, observer: Observer[Decision]): L.Button =
    Button(_.handledAs(Decision.New(newTask)) --> observer).amend(
      L.cls(Styles.`new`),
      "New task"
    )
}
