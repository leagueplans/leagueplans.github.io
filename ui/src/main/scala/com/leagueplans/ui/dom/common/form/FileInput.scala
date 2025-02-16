package com.leagueplans.ui.dom.common.form

import com.leagueplans.ui.utils.laminar.LaminarOps.handledAs
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.File

object FileInput {
  def apply(id: String, accept: String): (L.Input, L.Label, Signal[Option[File]]) = {
    val state = Var(Option.empty[File])
    (input(id, accept, state.writer), label(id), state.signal)
  }

  private def input(
    id: String,
    accept: String,
    filesObserver: Observer[Option[File]]
  ): L.Input =
    L.input(
      L.`type`("file"),
      L.idAttr(id),
      L.nameAttr(id),
      L.accept(accept),
      L.inContext(self =>
        L.onChange.handledAs(Option(self.ref.files.item(0))) --> filesObserver
      )
    )

  private def label(id: String): L.Label =
    L.label(L.forId(id))
}
