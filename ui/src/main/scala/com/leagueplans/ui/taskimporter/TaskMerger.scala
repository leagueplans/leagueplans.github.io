package com.leagueplans.ui.taskimporter

import com.leagueplans.common.model.LeagueTask

import scala.collection.mutable

object TaskMerger {
  def mergeExact(
    existingTasks: List[LeagueTask],
    newTasks: List[LeagueTask]
  ): List[DecisionMaker.Decision.Merge] = {
    val remaining = newTasks.map(task => (task.name, task.description) -> task).to(mutable.Map)

    existingTasks.collect(Function.unlift { existingTask =>
      val key = (existingTask.name, existingTask.description)
      remaining.get(key).map { newTask =>
        remaining.remove(key)
        DecisionMaker.Decision.Merge(
          existingTask,
          newTask,
          mergePrioExisting(existingTask, newTask)
        )
      }
    })
  }

  def mergePrioExisting(existingTask: LeagueTask, newTask: LeagueTask): LeagueTask =
    existingTask.copy(
      leagues1Props = existingTask.leagues1Props.orElse(newTask.leagues1Props),
      leagues2Props = existingTask.leagues2Props.orElse(newTask.leagues2Props),
      leagues3Props = existingTask.leagues3Props.orElse(newTask.leagues3Props),
      leagues4Props = existingTask.leagues4Props.orElse(newTask.leagues4Props),
      leagues5Props = existingTask.leagues5Props.orElse(newTask.leagues5Props)
    )

  def mergePrioNew(existingTask: LeagueTask, newTask: LeagueTask): LeagueTask =
    existingTask.copy(
      name = newTask.name,
      description = newTask.description,
      leagues1Props = existingTask.leagues1Props.orElse(newTask.leagues1Props),
      leagues2Props = existingTask.leagues2Props.orElse(newTask.leagues2Props),
      leagues3Props = existingTask.leagues3Props.orElse(newTask.leagues3Props),
      leagues4Props = existingTask.leagues4Props.orElse(newTask.leagues4Props),
      leagues5Props = existingTask.leagues5Props.orElse(newTask.leagues5Props)
    )
}
