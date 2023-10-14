package ddm.ui.dom.common.form

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringValueMapper}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StylisedRadio {
  def apply(
    element: L.Modifier[L.Label],
    checked: Signal[Boolean],
    radio: L.Input,
    label: L.Label
  ): L.Children =
    List(
      radio.amend(L.cls(Styles.radio)),
      label.amend(
        L.cls <-- checked.map {
          case true => Styles.selection
          case false => Styles.alternative
        },
        element
      )
    )

  @js.native @JSImport("/styles/common/form/stylisedRadio.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val radio: String = js.native
    val alternative: String = js.native
    val selection: String = js.native
  }
}
