package com.leagueplans.ui.dom.planning.player.task

import com.leagueplans.ui.dom.common.form.FuseSearch
import com.leagueplans.ui.dom.planning.player.task.TaskFilters.Filter
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskDetailsTab {
  def apply[Task](
    taskType: String,
    fuse: Fuse[Task],
    filtersSignal: Signal[List[Filter[?]]],
    toTaskList: (Signal[Option[Progress]], Signal[Option[List[Task]]]) => L.HtmlElement
  ): L.Div = {
    val progressVar = Var(Option.empty[Progress])

    val progressFilter = TaskFilters.Filter(
      id = "progress",
      label = "Progress:",
      List(Progress.Incomplete -> "Incomplete", Progress.Complete -> "Complete"),
      progressVar
    )

    val (search, _, searchResults) = FuseSearch(
      fuse,
      id = s"$taskType-task-fuse-search",
      initial = "",
      maxResults = fuse.elements.size
    )

    L.div(
      L.cls(Styles.tab),
      search.amend(L.cls(Styles.search), L.placeholder("search")),
      TaskFilters(
        taskType,
        filtersSignal.map(_ :+ progressFilter)
      ).amend(L.cls(Styles.filters)),
      toTaskList(progressVar.signal, searchResults).amend(L.cls(Styles.tasks))
    )
  }

  enum Progress {
    case Incomplete, Complete
  }

  @js.native @JSImport("/styles/planning/player/task/taskDetailsTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tab: String = js.native
    val search: String = js.native
    val tasks: String = js.native
    val filters: String = js.native
  }
}
