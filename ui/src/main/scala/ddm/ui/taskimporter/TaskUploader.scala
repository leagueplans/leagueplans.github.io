package ddm.ui.taskimporter

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, enrichSource, textToTextNode}
import ddm.common.model.LeagueTask
import ddm.ui.dom.common.form.JsonFileInput

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
      rawExistingTasksSignal --> Observer[Option[List[LeagueTask]]] {
        case Some(tasks) => tasksVar.update { case (_, newTasks) => (tasks, newTasks) }
        case None => ()
      },
      rawNewTasksSignal --> Observer[Option[List[LeagueTask]]] {
        case Some(tasks) => tasksVar.update { case (existingTasks, _) => (existingTasks, tasks) }
        case None => ()
      }
    )

    (node, tasksVar.signal)
  }
}
