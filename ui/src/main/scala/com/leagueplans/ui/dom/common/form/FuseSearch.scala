package com.leagueplans.ui.dom.common.form

import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L

object FuseSearch {
  def apply[T](
    fuse: Fuse[T],
    id: String,
    initial: String,
    maxResults: Int
  ): (L.Input, L.Label, Signal[Option[List[T]]])  = {
    val (input, label, query) = TextInput(TextInput.Type.Search, id, initial)

    val results = query.composeChanges(_.debounce(ms = 200)).map {
      case "" => None
      case q => Some(fuse.search(q, maxResults))
    }

    (input, label, results)
  }
}
