package com.leagueplans.ui.utils.dom

import org.scalajs.dom

import scala.scalajs.js.JavaScriptException

object DOMException {
  private def unapplyFor(name: String): Throwable => Option[String] = {
    case JavaScriptException(ex: dom.DOMException) if ex.name == name => Some(ex.message)
    case _ => None  
  }
  
  object NoModificationAllowed {
    def unapply(t: Throwable): Option[String] =
      unapplyFor("NoModificationAllowedError")(t)
  }
  
  object NotFound {
    def unapply(t: Throwable): Option[String] =
      unapplyFor("NotFoundError")(t)
  }
  
  object QuotaExceeded {
    def unapply(t: Throwable): Option[String] =
      unapplyFor("QuotaExceededError")(t)
  }
}
