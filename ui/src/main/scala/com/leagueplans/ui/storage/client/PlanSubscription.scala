package com.leagueplans.ui.storage.client

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step.ID
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.storage.client.PlanSubscription.{Message, Status}
import com.leagueplans.ui.storage.model.LamportTimestamp
import com.leagueplans.ui.storage.model.errors.{ProtocolError, UpdateError}
import com.leagueplans.ui.utils.airstream.ObservableOps.withKillSwitch
import com.raquo.airstream.core.EventStream
import com.raquo.airstream.state.{StrictSignal, Var}

object PlanSubscription {
  enum Status {
    case Idle
    case Busy
    case Closed
    case Failed(cause: UpdateError | ProtocolError)
  }
  
  enum Message {
    case Done
    case Error(cause: ProtocolError)
    case Update(lamport: LamportTimestamp, data: Forest.Update[Step.ID, Step] | Plan.Settings)
    case UpdateSuccessful(lamport: LamportTimestamp)
    case UpdateFailed(lamport: LamportTimestamp, reason: UpdateError)
  }
}

final class PlanSubscription(
  initialLamport: LamportTimestamp,
  messages: EventStream[Message],
  save: (LamportTimestamp, Forest.Update[Step.ID, Step] | Plan.Settings) => ?,
  unsubscribe: () => ?
) extends AutoCloseable {
  private var currentLamport = initialLamport
  private val internalStatus = Var(PlanSubscription.Status.Idle)
  private val upstream = messages.withKillSwitch(resetOnStop = false)
  private val upstreamKillSwitch = upstream.killSwitch
  
  val status: StrictSignal[PlanSubscription.Status] = internalStatus.signal
  
  val updates: EventStream[Forest.Update[ID, Step] | Plan.Settings] =
    upstream.collect(Function.unlift {
      case Message.Done =>
        upstreamKillSwitch.kill()
        internalStatus.set(Status.Closed)
        None

      case Message.Error(cause) =>
        upstreamKillSwitch.kill()
        internalStatus.set(Status.Failed(cause))
        None
        
      case Message.Update(lamport, update) =>
        if (lamport == currentLamport.increment) {
          currentLamport = lamport
          internalStatus.set(Status.Idle)
          Some(update)
        } else {
          fail(UpdateError.OutOfSync)
          None
        }
        
      case Message.UpdateSuccessful(lamport) =>
        if (lamport == currentLamport) internalStatus.set(Status.Idle)
        None
        
      case Message.UpdateFailed(_, reason) =>
        fail(reason)
        None
    })

  def save(update: Forest.Update[Step.ID, Step] | Plan.Settings): Unit =
    ifRunning { _ =>
      currentLamport = currentLamport.increment
      save(currentLamport, update)
      Status.Busy
    }

  def close(): Unit =
    ifRunning { _ =>
      unsubscribe()
      upstreamKillSwitch.kill()
      Status.Closed 
    }
  
  private def fail(cause: UpdateError): Unit =
    ifRunning { _ =>
      unsubscribe()
      upstreamKillSwitch.kill()
      Status.Failed(cause)
    }

  private def ifRunning(f: Status.Idle.type | Status.Busy.type => Status): Unit =
    internalStatus.update {
      case status @ (Status.Idle | Status.Busy) => f(status)
      case terminated @ (_: Status.Failed | Status.Closed) => terminated
    }
}
