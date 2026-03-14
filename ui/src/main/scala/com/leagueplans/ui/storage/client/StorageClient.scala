package com.leagueplans.ui.storage.client

import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.storage.model.errors.{DeletionError, FileSystemError}
import com.leagueplans.ui.storage.model.{PlanExport, PlanID, PlanMetadata}
import com.leagueplans.ui.storage.worker.StorageCoordinator
import com.leagueplans.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import com.leagueplans.ui.wrappers.workers.{MessagePortClient, WorkerFactory}
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}

import scala.collection.mutable

object StorageClient {
  val statusKey = "storage-client"

  def apply(): StorageClient = {
    val coordinator = startCoordinator()
    val worker =
      MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator](WorkerFactory.storageWorker())

    worker.setMessageHandler(message => coordinator.send(Right(message)))

    new StorageClient(
      message => coordinator.send(Left(message)),
      toSetMessageHandler(coordinator, worker)
    )
  }

  private def startCoordinator(): MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut] =
    MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut](WorkerFactory.storageCoordinator())

  private def toSetMessageHandler(
    coordinator: MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut],
    worker: MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator]
  ): (Outbound.ToClient => ?) => Unit =
    onResponse => coordinator.setMessageHandler {
      case Right(message) => worker.send(message)
      case Left(message) => onResponse(message)
    }
}

final class StorageClient(
  send: Inbound.ToCoordinator => Unit,
  setMessageHandler: (Outbound.ToClient => ?) => Unit
) {
  private var lastRequestID: Long = 0L
  private def nextID(): Long = {
    lastRequestID += 1
    lastRequestID
  }

  private object requests {
    val refreshPlans: mutable.Map[Long, Observer[Either[FileSystemError, Unit]]] =
      mutable.Map.empty

    val creates: mutable.Map[Long, Observer[Either[FileSystemError, PlanID]]] =
      mutable.Map.empty

    val fetches: mutable.Map[Long, Observer[Either[FileSystemError, PlanExport]]] =
      mutable.Map.empty

    val deletes: mutable.Map[Long, Observer[Either[DeletionError, Unit]]] =
      mutable.Map.empty

    val subscriptions: mutable.Map[Long, Observer[Either[FileSystemError, (Plan, PlanSubscription)]]] =
      mutable.Map.empty
  }

  private val subscriptionBus = EventBus[(PlanID, PlanSubscription.Message) | PlanSubscription.Message]()
  private val plansVar = Var[Map[PlanID, PlanMetadata]](Map.empty)
  private val statusVar = Var(StatusTracker.Status.Idle).distinct

  private def setBusy(): Unit =
    statusVar.set(StatusTracker.Status.Busy)

  private def updateStatus(failureReason: Option[String] = None): Unit =
    if (
      requests.refreshPlans.isEmpty &&
        requests.creates.isEmpty &&
        requests.fetches.isEmpty &&
        requests.deletes.isEmpty &&
        requests.subscriptions.isEmpty
    ) {
      statusVar.set(
        failureReason match {
          case None => StatusTracker.Status.Idle
          case Some(error) => StatusTracker.Status.Failed(error)
        }
      )
    }

  val plansSignal: StrictSignal[Map[PlanID, PlanMetadata]] =
    plansVar.signal

  val status: Signal[StatusTracker.Status] =
    statusVar.signal.distinct

  def refreshPlans(): StrictSignal[Option[Either[FileSystemError, Unit]]] = {
    val promise = triggerPlansRefresh()
    setBusy()
    promise
  }

  def create(metadata: PlanMetadata, plan: Plan): StrictSignal[Option[Either[FileSystemError, PlanID]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[FileSystemError, PlanID]]](None)
    requests.creates += requestID -> promise.someWriter
    send(Inbound.Create(requestID, metadata, plan))
    triggerPlansRefresh()
    setBusy()
    promise.signal
  }

  def fetch(id: PlanID): StrictSignal[Option[Either[FileSystemError, PlanExport]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[FileSystemError, PlanExport]]](None)
    requests.fetches += requestID -> promise.someWriter
    send(Inbound.Fetch(requestID, id))
    setBusy()
    promise.signal
  }

  def delete(id: PlanID): StrictSignal[Option[Either[DeletionError, Unit]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[DeletionError, Unit]]](None)
    requests.deletes += requestID -> promise.someWriter
    send(Inbound.Delete(requestID, id))
    triggerPlansRefresh()
    setBusy()
    promise.signal
  }

  def subscribe(id: PlanID): StrictSignal[Option[Either[FileSystemError, (Plan, PlanSubscription)]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[FileSystemError, (Plan, PlanSubscription)]]](None)
    requests.subscriptions += requestID -> promise.someWriter
    send(Inbound.Subscribe(requestID, id))
    setBusy()
    promise.signal
  }

  private def triggerPlansRefresh(): StrictSignal[Option[Either[FileSystemError, Unit]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[FileSystemError, Unit]]](None)
    requests.refreshPlans += requestID -> promise.someWriter
    send(Inbound.ListPlans(requestID))
    setBusy()
    promise.signal
  }

  setMessageHandler {
    case Outbound.Plans(requestID, data) =>
      requests.refreshPlans.remove(requestID).foreach { observer =>
        observer.onNext(Right(()))
        updateStatus()
      }
      plansVar.writer.onNext(data)

    case Outbound.ListPlansFailed(requestID, reason) =>
      requests.refreshPlans.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case Outbound.CreateSucceeded(requestID, planID) =>
      requests.creates.remove(requestID).foreach { observer =>
        observer.onNext(Right(planID))
        updateStatus()
      }

    case Outbound.CreateFailed(requestID, reason) =>
      requests.creates.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case Outbound.FetchSucceeded(requestID, _, plan) =>
      requests.fetches.remove(requestID).foreach { observer =>
        observer.onNext(Right(plan))
        updateStatus()
      }

    case Outbound.FetchFailed(requestID, _, reason) =>
      requests.fetches.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case Outbound.Subscription(requestID, planID, lamport, plan) =>
      requests.subscriptions.remove(requestID).foreach { observer =>
        val subscription = new PlanSubscription(
          lamport,
          subscriptionBus.events.collect {
            case (`planID`, message) => message
            case message: PlanSubscription.Message => message
          },
          save = (lamport, update) => send(Inbound.Update(planID, lamport, update)),
          unsubscribe = () => send(Inbound.Unsubscribe(nextID(), planID))
        )
        observer.onNext(Right((plan, subscription)))
        updateStatus()
      }

    case Outbound.SubscriptionFailed(requestID, _, reason) =>
      requests.subscriptions.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case Outbound.SubscriptionTerminated(planID) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.Done)
      )

    case Outbound.Update(planID, lamport, update) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.Update(lamport, update.merge))
      )

    case Outbound.UpdateSucceeded(planID, lamport) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.UpdateSuccessful(lamport))
      )

    case Outbound.UpdateFailed(planID, lamport, reason) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.UpdateFailed(lamport, reason))
      )

    case Outbound.DeleteSucceeded(requestID, _) =>
      requests.deletes.remove(requestID).foreach { observer =>
        observer.onNext(Right(()))
        updateStatus()
      }

    case Outbound.DeleteFailed(requestID, _, reason) =>
      requests.deletes.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case Outbound.ProtocolFailure(reason) =>
      subscriptionBus.writer.onNext(
        PlanSubscription.Message.Error(reason)
      )
  }
}
