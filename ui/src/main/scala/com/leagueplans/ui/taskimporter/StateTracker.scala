package com.leagueplans.ui.taskimporter

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.taskimporter.DecisionMaker.Decision
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.enrichSource
import com.raquo.laminar.modifiers.Binder

import scala.collection.immutable.SortedMap

object StateTracker {
  final case class State(
    processedTasks: List[LeagueTask],
    remainingExistingTasks: SortedMap[Int, LeagueTask],
    remainingNewTasks: SortedMap[Int, LeagueTask],
    nextNewTaskID: Int
  )

  def apply(
    initialExistingTasks: List[LeagueTask],
    initialNewTasks: List[LeagueTask],
    decisionStream: EventStream[DecisionMaker.Decision]
  ): (Binder.Base, Signal[State]) = {
    val initialState = State(
      processedTasks = List.empty,
      initialExistingTasks.map(task => task.id -> task).to(SortedMap),
      initialNewTasks.map(task => task.id -> task).to(SortedMap),
      nextNewTaskID = initialExistingTasks.map(_.id).maxOption.getOrElse(0) + 1
    )
    val stateVar = Var(initialState)
    // Debouncing here avoids repainting on changes to the state when processing the
    // initial exact task merges
    val signal = stateVar.signal.changes.debounce(ms = 10).toSignal(initialState)

    (decisionStream --> stateVar.updater(handle), signal)
  }

  private def handle(state: State, decision: DecisionMaker.Decision): State =
    decision match {
      case Decision.New(task) =>
        state.copy(
          processedTasks = state.processedTasks :+ task.copy(id = state.nextNewTaskID),
          remainingNewTasks = state.remainingNewTasks - task.id,
          nextNewTaskID = state.nextNewTaskID + 1
        )

      case Decision.Merge(existingTask, newTask, mergedTask) =>
        state.copy(
          processedTasks = state.processedTasks :+ mergedTask,
          remainingExistingTasks = state.remainingExistingTasks - existingTask.id,
          remainingNewTasks = state.remainingNewTasks - newTask.id
        )
    }
}
