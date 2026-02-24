package com.leagueplans.ui.wrappers.js

import com.leagueplans.ui.facades.js.AsyncIterator

import scala.scalajs.js

object AsyncIteratorOps {
  extension [T](self: AsyncIterator[T]) {
    def sequenced: js.Promise[List[T]] =
      js.async {
        var acc = List.empty[T]
        var next = js.await(self.next())
        while !next.done do {
          acc :+= next.value
          next = js.await(self.next())
        }
        acc
      }
  }
}
