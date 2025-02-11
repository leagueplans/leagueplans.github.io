package com.leagueplans.ui.utils.js

import scala.scalajs.js
import scala.scalajs.js.JavaScriptException

object TypeError {
  def unapply(t: Throwable): Option[String] =
    t match{
      case JavaScriptException(ex: js.TypeError) => Some(ex.message)
      case _ => None
    }
}
