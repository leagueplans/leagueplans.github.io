package com.leagueplans.ui.taskimporter

import com.leagueplans.common.model.LeagueTask
import com.leagueplans.ui.dom.common.form.JsonFileInput
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}

object TaskUploader {
  def apply(): (L.Div, Signal[(List[LeagueTask], List[LeagueTask])]) = {
    val (existingTasksInput, existingTasksLabel, rawExistingTasksSignal) =
      JsonFileInput[List[LeagueTask]]("existing-tasks-input")
    val (newTasksInput, newTasksLabel, rawNewTasksSignal) =
      JsonFileInput[List[LeagueTask]]("new-tasks-input")

    val tasksVar = Var((List.empty[LeagueTask], List.empty[LeagueTask]))

    val node = L.div(
      existingTasksLabel.amend("Existing tasks:"),
      existingTasksInput,
      newTasksLabel.amend("Tasks to merge in:"),
      newTasksInput,
      rawExistingTasksSignal --> {
        case Some(tasks) => tasksVar.update((_, newTasks) => (tasks, newTasks))
        case None => ()
      },
      rawNewTasksSignal --> {
        case Some(tasks) => tasksVar.update((existingTasks, _) => (existingTasks, tasks))
        case None => ()
      }
    )

    (node, tasksVar.signal)
  }
}
