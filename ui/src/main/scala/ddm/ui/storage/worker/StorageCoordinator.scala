package ddm.ui.storage.worker

import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner
import ddm.ui.storage.model.LamportTimestamp
import ddm.ui.storage.model.errors.{DeletionError, ProtocolError, UpdateError}
import ddm.ui.storage.worker.StorageCoordinator.*
import ddm.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import ddm.ui.utils.airstream.ObservableOps.flatMapConcat
import ddm.ui.wrappers.workers.{MessagePortClient, SharedWorkerScope}

import scala.concurrent.duration.DurationInt
import scala.reflect.TypeTest
import scala.scalajs.js.annotation.JSExportTopLevel
import scala.scalajs.js.timers

// I originally wanted the storage coordinator to start up its own dedicated worker
// for accessing the OPFS, and that decision motivated the entire design. However,
// it turns out that no browser currently supports starting dedicated workers from
// shared workers, though there is some discussion of the topic at
// https://github.com/whatwg/html/issues/8362#issuecomment-1385934634
//
// As a "workaround", I renamed the original implementation for the
// StorageCoordinator to IdealisedStorageCoordinator, and made this copy with small
// adjustments to get something working.
//
// The expected flow when using the below implementation looks like this:
//       Coordinator          Main thread        Storage worker
//            |      Request       |                    |
//         M1 | <----------------- |                    |
//            | Message for worker |                    |
//         M2 | -----------------> |                    |
//            |                    | Message for worker |
//            |                    | -----------------> | M3
//            |                    | Response for coord |
//            |                    | <----------------- | M4
//            | Response for coord |                    |
//         M5 | <----------------- |                    |
//            |      Response      |                    |
//         M6 | -----------------> |                    |
//
// In other words, the main thread spins up the dedicated storage worker on its
// side, and acts as a proxy for M2 and M4, redirecting these messages to the
// appropriate receiver. The current approach has a number of downsides:
// - messages M2 through M5 place additional (de)serialisation overhead on the main
//   thread
// - additional complexity for the main thread port implementation, as it needs to
//   understand how and when to do the proxying.
// - most importantly, the coordinator will hang if the browser tab is closed after
//   having sent M1 but before having sent M5. When the coordinator eventually
//   handles M2, it will wait for the response M5 before proceeding with any
//   further messages it has queued. In order for the coordinator to be restarted,
//   the user must close all tabs the app is open in, which means if the
//   coordinator does hang the user is likely to have an awful experience.
//   To mitigate this, a 30 second timer is started on the sending of M2. If this
//   timer elapses before the receipt of M5, then the coordinator aborts and
//   proceeds to the next message.
object StorageCoordinator {
  type MsgIn = Either[Inbound.ToCoordinator, Outbound.ToCoordinator]
  type MsgOut = Either[Outbound.ToClient, Inbound.ToWorker]
  private type Port = MessagePortClient[MsgOut, MsgIn]
  private type Result = Iterable[(Port, Outbound.ToClient)]

  @JSExportTopLevel("run", moduleID = "storagecoordinator")
  def run(): Unit = {
    val scope = new SharedWorkerScope[MsgOut, MsgIn]
    val messageBus = EventBus[(Port, Inbound.ToCoordinator)]()

    val subscriptions = PlanSubscriptions.empty[MsgOut, MsgIn]
    val setMessageHandler = toSetMessageHandler((port, message) =>
      messageBus.writer.onNext((port, message))
    )
    val coordinator = StorageCoordinator(subscriptions, setMessageHandler)

    messageBus
      .events
      .flatMapConcat(coordinator.handle)
      .addObserver(
        Observer(_.foreach((port, msg) => port.send(Left(msg))))
      )(using new ManualOwner)

    scope.setOnConnect(port =>
      setMessageHandler(port)(message =>
        onError(
          Outbound.ProtocolFailure(ProtocolError.UnexpectedMessage(message)),
          subscriptions
        ).foreach((port, message) => port.send(Left(message)))
      )
    )
  }

  private def toSetMessageHandler(
    onInboundToCoordinator: (Port, Inbound.ToCoordinator) => ?
  ): Port => (Outbound.ToCoordinator => ?) => Unit =
    port => onOutboundToCoordinator =>
      port.setMessageHandler {
        case Left(message) => onInboundToCoordinator(port, message)
        case Right(message) => onOutboundToCoordinator(message)
      }

  private def onError(
    error: Outbound.ProtocolFailure,
    subscriptions: PlanSubscriptions[MsgOut, MsgIn]
  ): Result = {
    subscriptions.all.flatMap { case (planID, (_, ports)) =>
      ports.flatMap(port =>
        subscriptions.deregister(port, planID)
        List((port, error), (port, Outbound.SubscriptionTerminated(planID)))
      )
    }
  }
}

private final class StorageCoordinator(
  subscriptions: PlanSubscriptions[MsgOut, MsgIn],
  setMessageHandler: Port => (Outbound.ToCoordinator => ?) => Unit
) {
  def handle(port: Port, message: Inbound.ToCoordinator): EventStream[Result] =
    message match {
      case Inbound.ListPlans => handleListPlans(port)
      case create: Inbound.Create => handleCreate(port, create)
      case fetch: Inbound.Fetch => handleFetch(port, fetch)
      case subscribe: Inbound.Subscribe => handleSubscribe(port, subscribe)
      case unsubscribe: Inbound.Unsubscribe => handleUnsubscribe(port, unsubscribe)
      case update: Inbound.Update => handleUpdate(port, update)
      case delete: Inbound.Delete => handleDelete(port, delete)
    }

  private def handleListPlans(port: Port): EventStream[Result] =
    deferToWorker[Outbound.ListPlansFailed | Outbound.Plans](port, Inbound.ListPlans)(resp =>
      List((port, resp))
    )

  private def handleCreate(port: Port, message: Inbound.Create): EventStream[Result] =
    deferToWorker[Outbound.CreateFailed | Outbound.CreateSucceeded](port, message)(resp =>
      List((port, resp))
    )

  private def handleFetch(port: Port, message: Inbound.Fetch): EventStream[Result] =
    deferToWorker[Outbound.FetchFailed | Outbound.FetchSucceeded](port, message)(resp =>
      List((port, resp))
    )

  private def handleSubscribe(port: Port, message: Inbound.Subscribe): EventStream[Result] =
    deferToWorker[Outbound.ReadFailed | Outbound.ReadSucceeded](port, Inbound.Read(message.planID)) {
      case Outbound.ReadFailed(_, reason) =>
        List((port, Outbound.SubscriptionFailed(message.planID, reason)))
      case Outbound.ReadSucceeded(_, plan) =>
        val lamportTimestamp = subscriptions.register(port, message.planID)
        List((port, Outbound.Subscription(message.planID, lamportTimestamp, plan)))
    }

  private def handleUnsubscribe(port: Port, message: Inbound.Unsubscribe): EventStream[Result] = {
    subscriptions.deregister(port, message.planID)
    lift((port, Outbound.SubscriptionTerminated(message.planID)))
  }

  // For the future -
  // CRDTs and OT are potential techniques to improve the coordinated update logic.
  // They'd remove the need entirely for rejecting updates, but they are not simple
  // techniques and may place a barrier to future app model changes.
  // Probably useful for the CV though!
  // https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type
  // https://en.wikipedia.org/wiki/Operational_transformation
  private def handleUpdate(port: Port, message: Inbound.Update): EventStream[Result] =
    subscriptions.get(message.planID) match {
      case Some((currentLamport, ports)) if ports.contains(port) =>
        if (currentLamport.increment == message.lamport)
          applyUpdate(port, message, ports - port)
        else {
          subscriptions.deregister(port, message.planID)
          lift(
            (port, Outbound.UpdateFailed(message.planID, message.lamport, UpdateError.OutOfSync)),
            (port, Outbound.SubscriptionTerminated(message.planID))
          )
        }

      case _ =>
        lift(
          (port, Outbound.UpdateFailed(message.planID, message.lamport, UpdateError.OutOfSync)),
          (port, Outbound.SubscriptionTerminated(message.planID))
        )
    }

  private def applyUpdate(
    sourcePort: Port,
    message: Inbound.Update,
    otherSubscribers: Set[Port]
  ): EventStream[Result] =
    deferToWorker[Outbound.UpdateFailed | Outbound.UpdateSucceeded](sourcePort, message) {
      case failure: Outbound.UpdateFailed =>
        // We've tried to write but hit a failure, so we don't know what state the
        // file system is in. The only safe thing to do is to invalidate all
        // subscriptions so that clients will resubscribe with fresh information
        // from the file system.
        val broadcast = Outbound.SubscriptionTerminated(message.planID)
        (otherSubscribers + sourcePort).map { port =>
          subscriptions.deregister(port, message.planID)
          (port, broadcast)
        }.toList.prepended((sourcePort, failure))

      case success: Outbound.UpdateSucceeded =>
        subscriptions.incrementLamport(message.planID)
        val broadcast = Outbound.Update(message.planID, message.lamport, message.update)
        otherSubscribers.map((_, broadcast)) +
          ((sourcePort, Outbound.UpdateSucceeded(message.planID, message.lamport)))
    }

  private def handleDelete(port: Port, message: Inbound.Delete): EventStream[Result] =
    if (subscriptions.get(message.planID).nonEmpty)
      lift((
        port,
        Outbound.DeleteFailed(message.planID, DeletionError.PlanOpenInAnotherWindow)
      ))
    else
      deferToWorker[Outbound.DeleteFailed | Outbound.DeleteSucceeded](port, message)(resp =>
        List((port, resp))
      )

  private def deferToWorker[Response <: Outbound.ToCoordinator](port: Port, message: Inbound.ToWorker)(
    handleResponse: Response => Result
  )(using TypeTest[Outbound.ToCoordinator | ProtocolError, Response]): EventStream[Result] = {
    val eventBus = EventBus[Outbound.ToCoordinator | ProtocolError]()
    
    val timeout = 30.seconds
    val timer = timers.setTimeout(timeout) {
      setMessageHandler(port)(_ => ())
      eventBus.writer.onNext(ProtocolError.Timeout(timeout))
    }
    
    setMessageHandler(port) { response =>
      timers.clearTimeout(timer)
      eventBus.writer.onNext(response)
    }
    port.send(Right(message))

    eventBus.events.map {
      case error: ProtocolError =>
        onError(Outbound.ProtocolFailure(error), subscriptions)

      case response: Response =>
        handleResponse(response)
        
      case unexpectedMessage: Outbound.ToCoordinator => 
        onError(Outbound.ProtocolFailure(ProtocolError.UnexpectedMessage(unexpectedMessage)), subscriptions)
    }
  }

  private def lift(messages: (Port, Outbound.ToClient)*): EventStream[Result] =
    EventStream.fromValue(messages, emitOnce = true)
}
