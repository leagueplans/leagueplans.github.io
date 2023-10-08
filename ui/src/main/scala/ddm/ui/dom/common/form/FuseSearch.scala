package ddm.ui.dom.common.form

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.L
import ddm.ui.wrappers.fusejs.Fuse

object FuseSearch {
  def apply[T](
    fuse: Fuse[T],
    id: String,
    maxResults: Int,
    defaultResults: List[T]
  ): (L.Input, L.Label, Signal[List[T]])  = {
    val (input, label, query) =
      TextInput(
        TextInput.Type.Search,
        id,
        initial = ""
      )

    val results = query.composeChanges(_.debounce(ms = 200)).map {
      case "" => defaultResults
      case q => fuse.search(q, maxResults)
    }

    (input, label, results)
  }
}
