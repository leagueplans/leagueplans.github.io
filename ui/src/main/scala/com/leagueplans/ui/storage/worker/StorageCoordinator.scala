package com.leagueplans.ui.storage.worker

import com.leagueplans.ui.storage.model.{LamportTimestamp, PlanID}
import com.leagueplans.ui.storage.model.errors.{DeletionError, SubscriptionError, UpdateError}
import com.leagueplans.ui.storage.worker.StorageCoordinator.*
import com.leagueplans.ui.storage.worker.StorageProtocol.{ToClient, ToCoordinator}
import com.leagueplans.uicommon.utils.airstream.ObservableOps.flatMapConcat
import com.leagueplans.uicommon.wrappers.locks.Locks
import com.leagueplans.uicommon.wrappers.workers.{MessagePortClient, SharedWorkerScope}
import com.raquo.airstream.core.{EventStream, Observer}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.ownership.ManualOwner

import scala.scalajs.js.annotation.JSExportTopLevel

// Every tab running this version of the app shares one coordinator. It handles their
// requests one at a time, tracks which tabs are subscribed to each plan, and sends
// each saved update to the plan's other subscribers.
//
// The coordinator reads and writes the OPFS itself, through the asynchronous file API.
// Sync access handles are only available in dedicated workers, which shared workers
// can't start in every browser, and handing the work to a worker started by a tab
// would make storage depend on that tab staying open.
object StorageCoordinator {
  private type Port = MessagePortClient[ToClient, ToCoordinator]
  private type Result = Iterable[(Port, ToClient)]

  @JSExportTopLevel("run", moduleID = "storagecoordinator")
  def run(): Unit = {
    val scope = new SharedWorkerScope[ToClient, ToCoordinator]
    val messageBus = EventBus[(Port, ToCoordinator)]()
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
}

private final class StorageCoordinator(
  subscriptions: PlanSubscriptions[Port],
  storage: PlanStorage
) {
  def handle(port: Port, message: ToCoordinator): EventStream[Result] =
    message match {
      case list: ToCoordinator.ListPlans => handleListPlans(port, list)
      case create: ToCoordinator.Create => handleCreate(port, create)
      case fetch: ToCoordinator.Fetch => handleFetch(port, fetch)
      case subscribe: ToCoordinator.Subscribe => handleSubscribe(port, subscribe)
      case unsubscribe: ToCoordinator.Unsubscribe => handleUnsubscribe(port, unsubscribe)
      case update: ToCoordinator.Update => handleUpdate(port, update)
      case delete: ToCoordinator.Delete => handleDelete(port, delete)
    }

  private def handleListPlans(port: Port, message: ToCoordinator.ListPlans): EventStream[Result] =
    storage.listPlans().map {
      case Left(reason) => List((port, ToClient.ListPlansFailed(message.requestID, reason)))
      case Right(plans) => List((port, ToClient.Plans(message.requestID, plans)))
    }

  private def handleCreate(port: Port, message: ToCoordinator.Create): EventStream[Result] =
    storage.create(message.metadata, message.plan).map {
      case Left(reason) => List((port, ToClient.CreateFailed(message.requestID, reason)))
      case Right(planID) => List((port, ToClient.CreateSucceeded(message.requestID, planID)))
    }

  private def handleFetch(port: Port, message: ToCoordinator.Fetch): EventStream[Result] =
    storage.fetch(message.planID).map {
      case Left(reason) => List((port, ToClient.FetchFailed(message.requestID, message.planID, reason)))
      case Right(plan) => List((port, ToClient.FetchSucceeded(message.requestID, message.planID, plan)))
    }

  // The plan is locked before it's read, so that another version of the app can't write to it
  // after we've read it
  private def handleSubscribe(port: Port, message: ToCoordinator.Subscribe): EventStream[Result] = {
    val planID = message.planID
    subscriptions.lock(planID, onTakenOver = notifyTakenOver(planID)).flatMapSwitch {
      case Left(reason) =>
        lift((port, ToClient.SubscriptionFailed(message.requestID, planID, SubscriptionError.LockUnavailable(reason))))

      case Right(()) =>
        storage.read(planID).map {
          case Left(reason) =>
            subscriptions.releaseIfUnused(planID)
            List((port, ToClient.SubscriptionFailed(message.requestID, planID, SubscriptionError.FileSystem(reason))))

          case Right(plan) =>
            val lamportTimestamp = subscriptions.register(port, planID)
            val subscription = (port, ToClient.Subscription(message.requestID, planID, lamportTimestamp, plan))
            // Another version of the app may have taken the plan while we were reading it
            if (subscriptions.isLocked(planID))
              List(subscription)
            else {
              subscriptions.deregister(port, planID)
              List(subscription, (port, ToClient.SubscriptionTakenOver(planID)))
            }
        }
    }
  }

  /** Another version of the app has taken the plan, and its subscribers have been dropped */
  private def notifyTakenOver(planID: PlanID)(ports: Set[Port]): Unit =
    ports.foreach(_.send(ToClient.SubscriptionTakenOver(planID)))

  private def handleUnsubscribe(port: Port, message: ToCoordinator.Unsubscribe): EventStream[Result] = {
    subscriptions.deregister(port, message.planID)
    lift((port, ToClient.SubscriptionTerminated(message.planID)))
  }

  // For the future -
  // CRDTs and OT are potential techniques to improve the coordinated update logic.
  // They'd remove the need entirely for rejecting updates, but they are not simple
  // techniques and may place a barrier to future app model changes.
  // Probably useful for the CV though!
  // https://en.wikipedia.org/wiki/Conflict-free_replicated_data_type
  // https://en.wikipedia.org/wiki/Operational_transformation
  private def handleUpdate(port: Port, message: ToCoordinator.Update): EventStream[Result] =
    subscriptions.get(message.planID) match {
      case Some((currentLamport, ports)) if ports.contains(port) =>
        if (currentLamport.increment == message.lamport)
          applyUpdate(port, message, ports - port)
        else {
          subscriptions.deregister(port, message.planID)
          lift(
            (port, ToClient.UpdateFailed(message.planID, message.lamport, UpdateError.OutOfSync)),
            (port, ToClient.SubscriptionTerminated(message.planID))
          )
        }

      case _ =>
        lift(
          (port, ToClient.UpdateFailed(message.planID, message.lamport, UpdateError.OutOfSync)),
          (port, ToClient.SubscriptionTerminated(message.planID))
        )
    }

  private def applyUpdate(
    sourcePort: Port,
    message: ToCoordinator.Update,
    otherSubscribers: Set[Port]
  ): EventStream[Result] =
    storage.applyUpdate(message.planID, message.update.merge).map {
      case Left(reason) =>
        // We've tried to write but hit a failure, so we don't know what state the
        // file system is in. The only safe thing to do is to invalidate all
        // subscriptions so that clients will resubscribe with fresh information
        // from the file system.
        val failure = ToClient.UpdateFailed(message.planID, message.lamport, UpdateError.FileSystem(reason))
        val broadcast = ToClient.SubscriptionTerminated(message.planID)
        (otherSubscribers + sourcePort).map { port =>
          subscriptions.deregister(port, message.planID)
          (port, broadcast)
        }.toList.prepended((sourcePort, failure))

      case Right(_) =>
        subscriptions.incrementLamport(message.planID)
        val broadcast = ToClient.Update(message.planID, message.lamport, message.update)
        otherSubscribers.map((_, broadcast)) +
          ((sourcePort, ToClient.UpdateSucceeded(message.planID, message.lamport)))
    }

  private def handleDelete(port: Port, message: ToCoordinator.Delete): EventStream[Result] = {
    def failed(reason: DeletionError): Result =
      List((port, ToClient.DeleteFailed(message.requestID, message.planID, reason)))

    subscriptions.whileUnused(message.planID)(
      ifInUse = failed(DeletionError.PlanOpenInAnotherWindow),
      ifFailed = reason => failed(DeletionError.LockUnavailable(reason))
    )(
      storage.delete(message.planID).map {
        case Left(reason) => failed(DeletionError.FileSystem(reason))
        case Right(_) => List((port, ToClient.DeleteSucceeded(message.requestID, message.planID)))
      }
    )
  }

  private def lift(messages: (Port, ToClient)*): EventStream[Result] =
    EventStream.fromValue(messages, emitOnce = true)
}
