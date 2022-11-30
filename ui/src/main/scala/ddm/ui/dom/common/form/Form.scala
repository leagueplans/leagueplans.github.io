package ddm.ui.dom.common.form

import com.raquo.airstream.core.Observer
import com.raquo.domtypes.jsdom.defs.events.TypedTargetEvent
import com.raquo.laminar.api.{L, eventPropToProcessor}
import org.scalajs.dom.html

object Form {
  def apply(observer: Observer[Unit]): (L.FormElement, L.Input) = {
    val submit = input
    val form = L.form(
      submit,
      L.onSubmit.preventDefault -->
        observer.contramap[TypedTargetEvent[html.Form]](_ => ())
    )
    (form, submit)
  }

  private def input: L.Input =
    L.input(L.`type`("submit"))
}
