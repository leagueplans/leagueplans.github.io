package com.raquo.airstream

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentStream}
import com.raquo.airstream.core.{Observable, Transaction}

import scala.collection.mutable
import scala.concurrent.duration.{Duration, FiniteDuration}
import scala.scalajs.js.timers.{SetTimeoutHandle, clearTimeout, setTimeout}

object DelayedToggleStream {
  enum Action[+ID] {
    case Open(id: ID, delay: FiniteDuration)
    case Close(id: ID, delay: FiniteDuration)
    case AbortAll extends Action[Nothing]
  }
}

final class DelayedToggleStream[T, ID](
  protected val parent: Observable[T],
  getAction: T => DelayedToggleStream.Action[ID]
) extends SingleParentStream[T, T] with InternalNextErrorObserver[T] {
  // The topoRank is reset to 1 because downstream events are emitted asynchronously in separate transactions
  protected val topoRank: Int = 1
  private val outstanding = mutable.Map.empty[ID, (DelayedToggleStream.Action[ID], SetTimeoutHandle)]

  protected def onNext(nextValue: T, transaction: Transaction): Unit = {
    val action = getAction(nextValue)
    action match {
      case DelayedToggleStream.Action.AbortAll =>
        cancelOutstanding()
        // New transaction because our topoRank is set to 1
        Transaction(fireValue(nextValue, _))

      case DelayedToggleStream.Action.Open(id, delay) =>
        cancel(id)
        schedule(nextValue, id, delay, action)

      case DelayedToggleStream.Action.Close(id, delay) =>
        outstanding.get(id) match {
          case Some((_: DelayedToggleStream.Action.Open[ID], timer)) => clearTimeout(timer)
          case Some(_) => /* Do nothing */
          case None => schedule(nextValue, id, delay, action)
        }
    }
  }
  
  private def schedule(
    value: T,
    id: ID,
    delay: FiniteDuration,
    action: DelayedToggleStream.Action[ID]
  ): Unit =
    if (delay == Duration.Zero) {
      // New transaction because our topoRank is set to 1
      Transaction(fireValue(value, _))
    } else {
      val timer = setTimeout(delay.toMillis.toDouble)(Transaction { tx =>
        outstanding.remove(id)
        fireValue(value, tx)
      })
      outstanding += id -> (action, timer)
    }

  protected def onError(nextError: Throwable, transaction: Transaction): Unit =
    fireError(nextError, transaction)

  override protected def onStop(): Unit = {
    cancelOutstanding()
    super.onStop()
  }
  
  private def cancelOutstanding(): Unit =
    outstanding.keys.foreach(cancel)

  private def cancel(id: ID): Unit =
    outstanding.remove(id).foreach((_, timer) =>
      clearTimeout(timer)
    )
}
