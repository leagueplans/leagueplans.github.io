package com.raquo.airstream

import com.raquo.airstream.common.{InternalNextErrorObserver, InternalTryObserver, SingleParentStream}
import com.raquo.airstream.core.{EventStream, Observable, Transaction}

import scala.annotation.nowarn
import scala.util.{Failure, Success, Try}

final class BufferedStream[In, Out](
  protected val parent: Observable[In],
  process: In => EventStream[Out]
) extends SingleParentStream[In, Out] with InternalNextErrorObserver[In] {
  protected val topoRank: Int = 1

  private var maybeProcessor = Option.empty[EventStream[Out]]
  private var buffer = List.empty[In]

  private val processorObserver = new InternalTryObserver[Out] {
    protected def onTry(nextValue: Try[Out], transaction: Transaction): Unit = {
      stopProcessing()
      buffer.headOption.foreach { bufferedValue =>
        buffer = buffer.drop(1)
        startProcessing(bufferedValue)
      }

      Transaction(trx =>
        nextValue match {
          case Failure(error) => fireError(error, trx)
          case Success(value) => fireValue(value, trx)
        }
      ): @nowarn("msg=discarded non-Unit value")
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
      buffer :+= nextValue
    else
      startProcessing(nextValue)

  protected def onError(nextError: Throwable, transaction: Transaction): Unit =
    fireError(nextError, transaction)
}
