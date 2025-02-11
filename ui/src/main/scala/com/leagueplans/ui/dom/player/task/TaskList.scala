package com.leagueplans.ui.dom.player.task

import com.leagueplans.ui.utils.HasID
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLOListElement

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskList {
  def apply[Task : HasID](
    tasksSignal: Signal[List[Task]],
    toNode: Task => L.Node
  ): ReactiveHtmlElement[HTMLOListElement] =
    L.ol(
      L.cls(Styles.list),
      L.children <-- tasksSignal.split(_.id)((_, task, _) =>
        L.li(L.cls(Styles.entry), toNode(task))
      )
    )

  @js.native @JSImport("/styles/player/task/taskList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val list: String = js.native
    val entry: String = js.native
  }
}
