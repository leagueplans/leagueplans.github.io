package com.raquo.airstream

import com.raquo.airstream.common.{InternalNextErrorObserver, InternalTryObserver, SingleParentStream}
import com.raquo.airstream.core.{EventStream, Observable, Transaction}

import scala.collection.mutable
import scala.util.Try

/** A stream implementation intended for running an asynchronous function sequentially on the
  * input stream.
  * 
  * @param process A function which is expected to convert an input into an EventStream that
  *                will eventually emit a single output value. The resulting EventStream will
  *                be disconnected after the first value is emitted.
  */
final class BufferedStream[In, Out](
  protected val parent: Observable[In],
  process: In => EventStream[Out]
) extends SingleParentStream[In, Out] with InternalNextErrorObserver[In] {
  // The topoRank is reset to 1 because downstream events are emitted asynchronously in separate transactions
  protected val topoRank: Int = 1

  private var maybeProcessor = Option.empty[EventStream[Out]]
  private val buffer = mutable.Queue.empty[In]

  private val processorObserver = new InternalTryObserver[Out] {
    protected def onTry(nextValue: Try[Out], transaction: Transaction): Unit = {
      stopProcessing()
      buffer.removeHeadOption().foreach(startProcessing)
      fireTry(nextValue, transaction)
    }
  }

  private def startProcessing(nextValue: In): Unit = {
    val processor = process(nextValue)
    maybeProcessor = Some(processor)
    processor.addInternalObserver(processorObserver, shouldCallMaybeWillStart = true)
  }

  private def stopProcessing(): Unit = {
    maybeProcessor.foreach(_.removeInternalObserver(processorObserver))
    maybeProcessor = None
  }

  override protected def onStop(): Unit = {
    stopProcessing()
    super.onStop()
  }

  protected def onNext(nextValue: In, transaction: Transaction): Unit =
    if (maybeProcessor.nonEmpty)
      buffer.addOne(nextValue)
    else
      startProcessing(nextValue)

  protected def onError(nextError: Throwable, transaction: Transaction): Unit =
    fireError(nextError, transaction)
}
