package ddm.ui.utils.js

import scalajs.js
import scalajs.js.JavaScriptException

object TypeError {
  def unapply(t: Throwable): Option[String] =
    t match{
      case JavaScriptException(ex: js.TypeError) => Some(ex.message)
      case _ => None
    }
}
