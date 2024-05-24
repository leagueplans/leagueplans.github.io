package ddm.ui.storage.worker

import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner
import ddm.ui.facades.workers.{CreateWorkerOptions, WorkerFactory}
import ddm.ui.storage.model.LamportTimestamp
import ddm.ui.storage.model.errors.{DeletionError, ProtocolError, UpdateError}
import ddm.ui.storage.worker.IdealisedStorageCoordinator.{InboundPort, Result, WorkerPort}
import ddm.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import ddm.ui.utils.airstream.ObservableOps.flatMapConcat
import ddm.ui.wrappers.workers.{MessagePortClient, SharedWorkerScope}

import scala.reflect.ClassTag

// This worker attempts to create its own dedicated worker for accessing OPFS. This
// currently isn't supported by any browser, but I've left the code around in case
// it becomes a viable option in the future.
private object IdealisedStorageCoordinator {
  private type InboundPort = MessagePortClient[Outbound.ToClient, Inbound.ToCoordinator]
  private type WorkerPort = MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator]
  private type Result = Iterable[(InboundPort, Outbound.ToClient)]

  def run(): Unit = {
    val scope = new SharedWorkerScope[Outbound.ToClient, Inbound.ToCoordinator]
    val messageBus = EventBus[(InboundPort, Inbound.ToCoordinator)]()
    val coordinator = IdealisedStorageCoordinator(
      PlanSubscriptions.empty,
      MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator](
        WorkerFactory.storageWorker(CreateWorkerOptions.storageWorker)
      )
    )

    messageBus
      .events
      .flatMapConcat(coordinator.handle)
      .addObserver(
        Observer(_.foreach((port, msg) => port.send(msg)))
      )(using new ManualOwner)

    scope.setOnConnect(port =>
      port.setMessageHandler(msg =>
        messageBus.writer.onNext((port, msg))
      )
    )
  }
}

private final class IdealisedStorageCoordinator(
  subscriptions: PlanSubscriptions[Outbound.ToClient, Inbound.ToCoordinator],
  storageWorker: WorkerPort
) {
  def handle(port: InboundPort, message: Inbound.ToCoordinator): EventStream[Result] =
    message match {
      case Inbound.ListPlans => handleListPlans(port)
      case create: Inbound.Create => handleCreate(port, create)
      case subscribe: Inbound.Subscribe => handleSubscribe(port, subscribe)
      case unsubscribe: Inbound.Unsubscribe => handleUnsubscribe(port, unsubscribe)
      case update: Inbound.Update => handleUpdate(port, update)
      case delete: Inbound.Delete => handleDelete(port, delete)
    }

  private def handleListPlans(port: InboundPort): EventStream[Result] =
    deferToWorker[Outbound.ListPlansFailed | Outbound.Plans](Inbound.ListPlans)(resp =>
      List((port, resp))
    )

  private def handleCreate(port: InboundPort, message: Inbound.Create): EventStream[Result] =
    deferToWorker[Outbound.CreateFailed | Outbound.CreateSucceeded](message)(resp =>
      List((port, resp))
    )

  private def handleSubscribe(port: InboundPort, message: Inbound.Subscribe): EventStream[Result] =
    deferToWorker[Outbound.ReadFailed | Outbound.ReadSucceeded](Inbound.Read(message.planID)) {
      case Outbound.ReadFailed(_, reason) =>
        List((port, Outbound.SubscriptionFailed(message.planID, reason)))
      case Outbound.ReadSucceeded(_, plan) =>
        val lamportTimestamp = subscriptions.register(port, message.planID)
        List((port, Outbound.Subscription(message.planID, lamportTimestamp, plan)))
    }

  private def handleUnsubscribe(port: InboundPort, message: Inbound.Unsubscribe): EventStream[Result] = {
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
  private def handleUpdate(port: InboundPort, message: Inbound.Update): EventStream[Result] =
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
    sourcePort: InboundPort,
    message: Inbound.Update,
    otherSubscribers: Set[InboundPort]
  ): EventStream[Result] =
    deferToWorker[Outbound.UpdateFailed | Outbound.UpdateSucceeded](message) {
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

  private def handleDelete(port: InboundPort, message: Inbound.Delete): EventStream[Result] =
    if (subscriptions.get(message.planID).nonEmpty)
      lift((
        port,
        Outbound.DeleteFailed(message.planID, DeletionError.PlanOpenInAnotherWindow)
      ))
    else
      deferToWorker[Outbound.DeleteFailed | Outbound.DeleteSucceeded](message)(resp =>
        List((port, resp))
      )

  private def deferToWorker[Response <: Outbound.ToCoordinator : ClassTag](
    message: Inbound.ToWorker
  )(handleResponse: Response => Result): EventStream[Result] = {
    val eventBus = EventBus[Outbound.ToCoordinator]()
    storageWorker.setMessageHandler(eventBus.writer.onNext)
    storageWorker.send(message)

    eventBus.events.map {
      case response: Response => handleResponse(response)
      case unexpectedMessage =>
        val error = Outbound.ProtocolFailure(ProtocolError.UnexpectedMessage(unexpectedMessage))
        subscriptions.all.flatMap { case (planID, (_, ports)) =>
          ports.flatMap(port =>
            subscriptions.deregister(port, planID)
            List((port, error), (port, Outbound.SubscriptionTerminated(planID)))
          )
        }
    }
  }

  private def lift(messages: (InboundPort, Outbound.ToClient)*): EventStream[Result] =
    EventStream.fromValue(messages, emitOnce = true)
}
