package com.leagueplans.ui.storage.client

import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.status.StatusTracker
import com.leagueplans.ui.storage.model.errors.{DeletionError, FileSystemError, SubscriptionError}
import com.leagueplans.ui.storage.model.{LoadProgress, PlanExport, PlanID, PlanMetadata}
import com.leagueplans.ui.storage.worker.StorageProtocol.{ToClient, ToCoordinator}
import com.leagueplans.ui.wrappers.workers.WorkerFactory
import com.leagueplans.uicommon.wrappers.workers.MessagePortClient
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}

import scala.collection.mutable

object StorageClient {
  val statusKey = "storage-client"

  def apply(): StorageClient =
    new StorageClient(
      MessagePortClient[ToCoordinator, ToClient](WorkerFactory.storageCoordinator())
    )
}

final class StorageClient(coordinator: MessagePortClient[ToCoordinator, ToClient]) {
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

    val subscriptions: mutable.Map[Long, SubscribeRequest] =
      mutable.Map.empty

    /** Subscribe requests that were cancelled, which the coordinator may still reply to */
    val cancelledSubscriptions: mutable.Set[Long] =
      mutable.Set.empty
  }

  private final class SubscribeRequest(
    val planID: PlanID,
    val result: Var[Option[Either[SubscriptionError, (Plan, PlanSubscription)]]],
    val progress: Var[Option[LoadProgress]]
  )

  private val subscriptionBus = EventBus[(PlanID, PlanSubscription.Message) | PlanSubscription.Message]()
  private val plansVar = Var[Map[PlanID, PlanMetadata]](Map.empty)
  private val unreadablePlansVar = Var(Set.empty[PlanID])
  private val statusVar = Var[StatusTracker.Status](StatusTracker.Status.Idle).distinct

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

  /** Plans whose metadata couldn't be read, which can't be opened but can be downloaded */
  val unreadablePlansSignal: StrictSignal[Set[PlanID]] =
    unreadablePlansVar.signal

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
    coordinator.send(ToCoordinator.Create(requestID, metadata, plan))
    triggerPlansRefresh()
    setBusy()
    promise.signal
  }

  def fetch(id: PlanID): StrictSignal[Option[Either[FileSystemError, PlanExport]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[FileSystemError, PlanExport]]](None)
    requests.fetches += requestID -> promise.someWriter
    coordinator.send(ToCoordinator.Fetch(requestID, id))
    setBusy()
    promise.signal
  }

  def delete(id: PlanID): StrictSignal[Option[Either[DeletionError, Unit]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[DeletionError, Unit]]](None)
    requests.deletes += requestID -> promise.someWriter
    coordinator.send(ToCoordinator.Delete(requestID, id))
    triggerPlansRefresh()
    setBusy()
    promise.signal
  }

  def subscribe(id: PlanID): PendingSubscription = {
    val requestID = nextID()
    val request = SubscribeRequest(id, Var(None), Var(None))
    requests.subscriptions += requestID -> request
    coordinator.send(ToCoordinator.Subscribe(requestID, id))
    setBusy()
    PendingSubscription(id, request.result.signal, request.progress.signal, () => cancelSubscribe(requestID))
  }

  // If the reply is already on its way, the coordinator will ignore the cancellation and the
  // reply will be dealt with when it arrives
  private def cancelSubscribe(requestID: Long): Unit =
    requests.subscriptions.remove(requestID).foreach { request =>
      requests.cancelledSubscriptions += requestID
      coordinator.send(ToCoordinator.CancelSubscribe(requestID))
      request.result.set(Some(Left(SubscriptionError.Cancelled)))
      updateStatus()
    }

  private def triggerPlansRefresh(): StrictSignal[Option[Either[FileSystemError, Unit]]] = {
    val requestID = nextID()
    val promise = Var[Option[Either[FileSystemError, Unit]]](None)
    requests.refreshPlans += requestID -> promise.someWriter
    coordinator.send(ToCoordinator.ListPlans(requestID))
    setBusy()
    promise.signal
  }

  coordinator.setMessageHandler {
    // The plans are updated first, so that they're current for whoever's waiting on the refresh
    case ToClient.Plans(requestID, plans) =>
      unreadablePlansVar.set(plans.unreadable)
      plansVar.writer.onNext(plans.readable)
      requests.refreshPlans.remove(requestID).foreach { observer =>
        observer.onNext(Right(()))
        updateStatus()
      }

    case ToClient.ListPlansFailed(requestID, reason) =>
      requests.refreshPlans.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case ToClient.CreateSucceeded(requestID, planID) =>
      requests.creates.remove(requestID).foreach { observer =>
        observer.onNext(Right(planID))
        updateStatus()
      }

    case ToClient.CreateFailed(requestID, reason) =>
      requests.creates.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case ToClient.FetchSucceeded(requestID, _, plan) =>
      requests.fetches.remove(requestID).foreach { observer =>
        observer.onNext(Right(plan))
        updateStatus()
      }

    case ToClient.FetchFailed(requestID, _, reason) =>
      requests.fetches.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }

    case ToClient.SubscribeProgress(requestID, stepsRead, totalSteps) =>
      requests.subscriptions.get(requestID).foreach(
        _.progress.set(Some(LoadProgress(stepsRead, totalSteps)))
      )

    case ToClient.Subscription(requestID, planID, _, _) if requests.cancelledSubscriptions.remove(requestID) =>
      // Nobody wants the subscription, so give it back. The coordinator tracks subscribers by
      // tab, not by request, so it's kept if this tab has asked for the plan again since, such as
      // when switching away and straight back. That request's reply comes after this one.
      if (!requests.subscriptions.valuesIterator.exists(_.planID == planID))
        coordinator.send(ToCoordinator.Unsubscribe(nextID(), planID))

    case ToClient.Subscription(requestID, planID, lamport, plan) =>
      requests.subscriptions.remove(requestID).foreach { request =>
        val subscription = new PlanSubscription(
          planID,
          lamport,
          subscriptionBus.events.collect {
            case (`planID`, message) => message
            case message: PlanSubscription.Message => message
          },
          save = (lamport, update) => coordinator.send(ToCoordinator.Update(planID, lamport, update)),
          unsubscribe = () => coordinator.send(ToCoordinator.Unsubscribe(nextID(), planID))
        )
        request.result.set(Some(Right((plan, subscription))))
        updateStatus()
      }

    case ToClient.SubscriptionFailed(requestID, _, _) if requests.cancelledSubscriptions.remove(requestID) =>
      ()

    case ToClient.SubscriptionFailed(requestID, _, reason) =>
      requests.subscriptions.remove(requestID).foreach { request =>
        request.result.set(Some(Left(reason)))
        updateStatus(failureReason = Some(reason.message))
      }

    case ToClient.SubscriptionTerminated(planID) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.Done)
      )

    case ToClient.SubscriptionTakenOver(planID) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.TakenOver)
      )

    case ToClient.Update(planID, lamport, update) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.Update(lamport, update.toUnion))
      )

    case ToClient.UpdateSucceeded(planID, lamport) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.UpdateSuccessful(lamport))
      )

    case ToClient.UpdateFailed(planID, lamport, reason) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.UpdateFailed(lamport, reason))
      )

    case ToClient.DeleteSucceeded(requestID, _) =>
      requests.deletes.remove(requestID).foreach { observer =>
        observer.onNext(Right(()))
        updateStatus()
      }

    case ToClient.DeleteFailed(requestID, _, reason) =>
      requests.deletes.remove(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        updateStatus(failureReason = Some(reason.message))
      }
  }
}
