package com.leagueplans.ui.wrappers.js

import com.leagueplans.ui.facades.js.AsyncIterator
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.given

import scala.concurrent.Future
import scala.scalajs.js
import scala.scalajs.js.JSConverters.JSRichFutureNonThenable

object AsyncIteratorOps {
  extension [T](self: AsyncIterator[T]) {

    // This implementation requires js.async/await, which aren't yet available for Scala 3
    //      js.async {
    //        var acc = List.empty[T]
    //        var next = js.await(self.next())
    //        while !next.done do {
    //          acc :+= next.value
    //          next = js.await(self.next())
    //        }
    //        acc
    //      }
    def sequenced: js.Promise[List[T]] =
      sequencedHelper(self, acc = List.empty).toJSPromise
  }
  
  private def sequencedHelper[T](
    iterator: AsyncIterator[T],
    acc: List[T],
  ): Future[List[T]] =
    iterator.next().toFuture.flatMap(entry =>
      if (entry.done)
        Future.successful(acc)
      else 
        sequencedHelper(iterator, acc :+ entry.value)
    )
}
