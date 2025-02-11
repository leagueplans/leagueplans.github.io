package com.leagueplans.ui.utils.airstream

import com.raquo.airstream.core.EventStream

import scala.scalajs.js.Promise

object JsPromiseOps {
  extension [T](self: Promise[T]) {
    def asObservable: EventStream[T] =
      EventStream.fromJsPromise(self, emitOnce = true)
  }
}
