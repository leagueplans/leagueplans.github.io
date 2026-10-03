package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.{LamportTimestamp, PlanID}
import com.leagueplans.ui.storage.model.errors.{DeletionError, ProtocolError, SubscriptionError, UpdateError}
import com.leagueplans.ui.storage.worker.StorageCoordinator.*
import com.leagueplans.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import com.leagueplans.uicommon.utils.airstream.ObservableOps.flatMapConcat
import com.leagueplans.uicommon.wrappers.locks.Locks
import com.leagueplans.uicommon.wrappers.workers.{MessagePortClient, SharedWorkerScope}
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner

import scala.reflect.TypeTest
import scala.scalajs.js.annotation.JSExportTopLevel

// The coordinator accesses the OPFS itself, through the asynchronous file API.
//
// It used to hand file system work to a dedicated worker instead, since sync access
// handles are only available in dedicated workers. But shared workers can't start
// dedicated workers in every browser, so each tab had to start one and pass messages
// between it and the coordinator. That made the coordinator depend on the tab staying
// open, and staying responsive, until the work finished.
object StorageCoordinator {
  private type Port = MessagePortClient[Outbound.ToClient, Inbound.ToCoordinator]
  private type Result = Iterable[(Port, Outbound.ToClient)]

  @JSExportTopLevel("run", moduleID = "storagecoordinator")
  def run(): Unit = {
    val scope = new SharedWorkerScope[Outbound.ToClient, Inbound.ToCoordinator]
    val messageBus = EventBus[(Port, Inbound.ToCoordinator)]()
    val coordinator = StorageCoordinator(PlanSubscriptions[Port](Locks.web), PlanStorage(scope))

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

  private def onError(
    error: Outbound.ProtocolFailure,
    subscriptions: PlanSubscriptions[Port]
  ): Result =
    subscriptions.all.flatMap { case (planID, (_, ports)) =>
      ports.flatMap { port =>
        subscriptions.deregister(port, planID)
        List((port, error), (port, Outbound.SubscriptionTerminated(planID)))
      }
    }
}

private final class StorageCoordinator(
  subscriptions: PlanSubscriptions[Port],
  storage: PlanStorage
) {
  def handle(port: Port, message: Inbound.ToCoordinator): EventStream[Result] =
    message match {
      case list: Inbound.ListPlans => handleListPlans(port, list)
      case create: Inbound.Create => handleCreate(port, create)
      case fetch: Inbound.Fetch => handleFetch(port, fetch)
      case subscribe: Inbound.Subscribe => handleSubscribe(port, subscribe)
      case unsubscribe: Inbound.Unsubscribe => handleUnsubscribe(port, unsubscribe)
      case update: Inbound.Update => handleUpdate(port, update)
      case delete: Inbound.Delete => handleDelete(port, delete)
    }

  private def handleListPlans(port: Port, message: Inbound.ListPlans): EventStream[Result] =
    deferToStorage[Outbound.ListPlansFailed | Outbound.Plans](message)(resp =>
      List((port, resp))
    )

  private def handleCreate(port: Port, message: Inbound.Create): EventStream[Result] =
    deferToStorage[Outbound.CreateFailed | Outbound.CreateSucceeded](message)(resp =>
      List((port, resp))
    )

  private def handleFetch(port: Port, message: Inbound.Fetch): EventStream[Result] =
    deferToStorage[Outbound.FetchFailed | Outbound.FetchSucceeded](message)(resp =>
      List((port, resp))
    )

  // The plan is locked before it's read, so that another version of the app can't write to it
  // after we've read it
  private def handleSubscribe(port: Port, message: Inbound.Subscribe): EventStream[Result] = {
    val planID = message.planID
    subscriptions.lock(planID, onTakenOver = notifyTakenOver(planID)).flatMapSwitch {
      case Left(reason) =>
        lift((port, Outbound.SubscriptionFailed(message.requestID, planID, SubscriptionError.LockUnavailable(reason))))

      case Right(()) =>
        deferToStorage[Outbound.ReadFailed | Outbound.ReadSucceeded](Inbound.Read(planID)) {
          case Outbound.ReadFailed(_, reason) =>
            subscriptions.releaseIfUnused(planID)
            List((port, Outbound.SubscriptionFailed(message.requestID, planID, SubscriptionError.FileSystem(reason))))

          case Outbound.ReadSucceeded(_, plan) =>
            val lamportTimestamp = subscriptions.register(port, planID)
            val subscription = (port, Outbound.Subscription(message.requestID, planID, lamportTimestamp, plan))
            // Another version of the app may have taken the plan while we were reading it
            if (subscriptions.isLocked(planID))
              List(subscription)
            else {
              subscriptions.deregister(port, planID)
              List(subscription, (port, Outbound.SubscriptionTakenOver(planID)))
            }
        }
    }
  }

  /** Another version of the app has taken the plan, and its subscribers have been dropped */
  private def notifyTakenOver(planID: PlanID)(ports: Set[Port]): Unit =
    ports.foreach(_.send(Outbound.SubscriptionTakenOver(planID)))

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
    deferToStorage[Outbound.UpdateFailed | Outbound.UpdateSucceeded](message) {
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

      case _: Outbound.UpdateSucceeded =>
        subscriptions.incrementLamport(message.planID)
        val broadcast = Outbound.Update(message.planID, message.lamport, message.update)
        otherSubscribers.map((_, broadcast)) +
          ((sourcePort, Outbound.UpdateSucceeded(message.planID, message.lamport)))
    }

  private def handleDelete(port: Port, message: Inbound.Delete): EventStream[Result] = {
    def failed(reason: DeletionError): Result =
      List((port, Outbound.DeleteFailed(message.requestID, message.planID, reason)))

    subscriptions.whileUnused(message.planID)(
      ifInUse = failed(DeletionError.PlanOpenInAnotherWindow),
      ifFailed = reason => failed(DeletionError.LockUnavailable(reason))
    )(
      deferToStorage[Outbound.DeleteFailed | Outbound.DeleteSucceeded](message)(resp =>
        List((port, resp))
      )
    )
  }

  private def deferToStorage[Response <: Outbound.ToCoordinator](message: Inbound.ToWorker)(
    handleResponse: Response => Result
  )(using TypeTest[Outbound.ToCoordinator, Response]): EventStream[Result] =
    storage.handle(message).map {
      case response: Response =>
        handleResponse(response)

      case unexpectedMessage =>
        onError(Outbound.ProtocolFailure(ProtocolError.UnexpectedMessage(unexpectedMessage)), subscriptions)
    }

  private def lift(messages: (Port, Outbound.ToClient)*): EventStream[Result] =
    EventStream.fromValue(messages, emitOnce = true)
}
