package com.leagueplans.ui.dom.planning.player.task

import com.leagueplans.ui.dom.common.form.Select
import com.raquo.airstream.core.Signal
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskFilters {
  final case class Filter[T](
    id: String,
    label: String,
    opts: List[(T, String)],
    state: Var[Option[T]]
  )

  def apply(id: String, filtersSignal: Signal[List[Filter[?]]]): L.Div =
    L.div(
      L.cls(Styles.section),
      L.h4(L.cls(PanelStyles.header, Styles.header), "Filters"),
      L.children <-- filtersSignal.split(_.id)((_, filter, _) =>
        toNode(id, filter)
      )
    )

  @js.native @JSImport("/styles/planning/player/task/taskFilters.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val header: String = js.native

    val filter: String = js.native
    val label: String = js.native
    val input: String = js.native
  }

  @js.native @JSImport("/styles/planning/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val header: String = js.native
  }

  private def toNode[T](id: String, filter: Filter[T]): L.Div = {
    val (input, label) =
      Select(
        id = s"task-filter-$id-${filter.id}",
        Select.Opt(None, "Any") +: filter.opts.map((t, name) =>
          Select.Opt(Some(t), name)
        ),
        filter.state
      )

    L.div(
      L.cls(Styles.filter),
      label.amend(L.cls(Styles.label), filter.label),
      input.amend(L.cls(Styles.input))
    )
  }
}
