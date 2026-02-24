package com.leagueplans.ui.utils.airstream

import com.raquo.airstream.core.EventStream

import scala.scalajs.js.Promise

object JsPromiseOps {
  // TODO Remove this in favour of js.async/await, once
  //      https://github.com/scala/scala3/issues/25342
  //      has been resolved
  extension [T](self: Promise[T]) {
    def asObservable: EventStream[T] =
      EventStream.fromJsPromise(self, emitOnce = true)
  }
}
