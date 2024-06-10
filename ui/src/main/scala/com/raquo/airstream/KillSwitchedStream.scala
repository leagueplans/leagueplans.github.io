package com.raquo.airstream

import com.raquo.airstream.common.{InternalNextErrorObserver, SingleParentStream}
import com.raquo.airstream.core.{Observable, Protected, Transaction}

object KillSwitchedStream {
  sealed trait KillSwitch {
    def kill(): Unit
  }
}

final class KillSwitchedStream[T](
  protected val parent: Observable[T],
  resetOnStop: Boolean
) extends SingleParentStream[T, T] with InternalNextErrorObserver[T] { stream =>
  protected val topoRank: Int = Protected.topoRank(parent) + 1
  
  private var permanentlyKilled = false

  override protected def onWillStart(): Unit =
    if (!permanentlyKilled) super.onWillStart()

  override protected def onStart(): Unit =
    if (!permanentlyKilled) super.onStart()
  
  def killSwitch: KillSwitchedStream.KillSwitch =
    new KillSwitchedStream.KillSwitch {
      def kill(): Unit = {
        permanentlyKilled = !resetOnStop
        parent.removeInternalObserver(stream)
      }
    }

  protected def onNext(nextValue: T, transaction: Transaction): Unit =
    fireValue(nextValue, transaction)

  protected def onError(nextError: Throwable, transaction: Transaction): Unit =
    fireError(nextError, transaction)
}
