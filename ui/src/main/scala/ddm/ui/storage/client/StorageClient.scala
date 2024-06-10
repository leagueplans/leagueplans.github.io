package ddm.ui.storage.client

import com.raquo.airstream.core.Observer
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}
import ddm.ui.facades.workers.{CreateWorkerOptions, WorkerFactory}
import ddm.ui.model.plan.Plan
import ddm.ui.storage.model.errors.{DeletionError, FileSystemError}
import ddm.ui.storage.model.{PlanExport, PlanID, PlanMetadata}
import ddm.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import ddm.ui.storage.worker.StorageCoordinator
import ddm.ui.wrappers.workers.MessagePortClient

import java.util.UUID
import scala.collection.mutable

object StorageClient {
  def apply(): StorageClient = {
    val coordinator = startCoordinator()
    val worker =
      MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator](
        WorkerFactory.storageWorker(CreateWorkerOptions.storageWorker)
      )
      
    worker.setMessageHandler(message => coordinator.send(Right(message)))

    new StorageClient(
      message => coordinator.send(Left(message)),
      toSetMessageHandler(coordinator, worker)
    )
  }
  
  private def startCoordinator(): MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut] =
    MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut](
      WorkerFactory.storageCoordinator(CreateWorkerOptions.storageCoordinator)
    )
  
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
  private object requests {
    val refreshPlans: mutable.Set[Observer[Either[FileSystemError, Unit]]] =
      mutable.Set.empty
    
    val creates: mutable.Map[String, Observer[Either[FileSystemError, PlanID]]] =
      mutable.Map.empty

    val fetches: mutable.Map[PlanID, List[Observer[Either[FileSystemError, PlanExport]]]] =
      mutable.Map.empty

    val deletes: mutable.Map[PlanID, List[Observer[Either[DeletionError, Unit]]]] =
      mutable.Map.empty

    val subscriptions: mutable.Map[PlanID, List[Observer[Either[FileSystemError, (Plan, PlanSubscription)]]]] =
      mutable.Map.empty
  }

  private val subscriptionBus = EventBus[(PlanID, PlanSubscription.Message) | PlanSubscription.Message]()
  private val plansVar = Var[Map[PlanID, PlanMetadata]](Map.empty)
  
  val plansSignal: StrictSignal[Map[PlanID, PlanMetadata]] =
    plansVar.signal

  def refreshPlans(): StrictSignal[Option[Either[FileSystemError, Unit]]] = {
    val promise = Var[Option[Either[FileSystemError, Unit]]](None)
    requests.refreshPlans += promise.someWriter
    send(Inbound.ListPlans)
    promise.signal
  }

  def create(metadata: PlanMetadata, plan: Plan): StrictSignal[Option[Either[FileSystemError, PlanID]]] = {
    val requestID = UUID.randomUUID().toString
    val promise = Var[Option[Either[FileSystemError, PlanID]]](None)
    requests.creates += requestID -> promise.someWriter
    send(Inbound.Create(requestID, metadata, plan))
    send(Inbound.ListPlans)
    promise.signal
  }

  def fetch(id: PlanID): StrictSignal[Option[Either[FileSystemError, PlanExport]]] = {
    val promise = Var[Option[Either[FileSystemError, PlanExport]]](None)
    requests.fetches += id -> (requests.fetches.getOrElse(id, List.empty) :+ promise.someWriter)
    send(Inbound.Fetch(id))
    promise.signal
  }

  def delete(id: PlanID): StrictSignal[Option[Either[DeletionError, Unit]]] = {
    val promise = Var[Option[Either[DeletionError, Unit]]](None)
    requests.deletes += id -> (requests.deletes.getOrElse(id, List.empty) :+ promise.someWriter)
    send(Inbound.Delete(id))
    send(Inbound.ListPlans)
    promise.signal
  }

  def subscribe(id: PlanID): StrictSignal[Option[Either[FileSystemError, (Plan, PlanSubscription)]]] = {
    val promise = Var[Option[Either[FileSystemError, (Plan, PlanSubscription)]]](None)
    requests.subscriptions += id -> (requests.subscriptions.getOrElse(id, List.empty) :+ promise.someWriter)
    send(Inbound.Subscribe(id))
    promise.signal
  }

  setMessageHandler {
    case Outbound.Plans(data) =>
      requests.refreshPlans.foreach { observer =>
        observer.onNext(Right(()))
        requests.refreshPlans -= observer
      }
      plansVar.writer.onNext(data)

    case Outbound.ListPlansFailed(reason) =>
      requests.refreshPlans.foreach { observer =>
        observer.onNext(Left(reason))
        requests.refreshPlans -= observer
      }

    case Outbound.CreateSucceeded(requestID, planID) =>
      requests.creates.get(requestID).foreach { observer =>
        observer.onNext(Right(planID))
        requests.creates -= requestID
      }

    case Outbound.CreateFailed(requestID, reason) =>
      requests.creates.get(requestID).foreach { observer =>
        observer.onNext(Left(reason))
        requests.creates -= requestID
      }

    case Outbound.FetchSucceeded(planID, plan) =>
      requests.fetches.get(planID).foreach { observers =>
        observers.foreach(_.onNext(Right(plan)))
        requests.fetches -= planID
      }

    case Outbound.FetchFailed(planID, reason) =>
      requests.fetches.get(planID).foreach { observers =>
        observers.foreach(_.onNext(Left(reason)))
        requests.fetches -= planID
      }

    case Outbound.Subscription(planID, lamport, plan) =>
      requests.subscriptions.get(planID).foreach { observers =>
        observers.foreach { observer =>
          val subscription = new PlanSubscription(
            lamport,
            subscriptionBus.events.collect {
              case (`planID`, message) => message
              case message: PlanSubscription.Message => message
            },
            save = (lamport, update) => send(Inbound.Update(planID, lamport, update)),
            unsubscribe = () => send(Inbound.Unsubscribe(planID))
          )
          observer.onNext(Right((plan, subscription)))
        }
        requests.subscriptions -= planID
      }

    case Outbound.SubscriptionFailed(planID, reason) =>
      requests.subscriptions.get(planID).foreach { observers =>
        observers.foreach(_.onNext(Left(reason)))
        requests.subscriptions -= planID
      }

    case Outbound.SubscriptionTerminated(planID) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.Done)
      )

    case Outbound.Update(planID, lamport, update) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.Update(lamport, update))
      )

    case Outbound.UpdateSucceeded(planID, lamport) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.UpdateSuccessful(lamport))
      )

    case Outbound.UpdateFailed(planID, lamport, reason) =>
      subscriptionBus.writer.onNext(
        (planID, PlanSubscription.Message.UpdateFailed(lamport, reason))
      )

    case Outbound.DeleteSucceeded(planID) =>
      requests.deletes.get(planID).foreach { observers =>
        observers.foreach(_.onNext(Right(())))
        requests.deletes -= planID
      }

    case Outbound.DeleteFailed(planID, reason) =>
      requests.deletes.get(planID).foreach { observers =>
        observers.foreach(_.onNext(Left(reason)))
        requests.deletes -= planID
      }

    case Outbound.ProtocolFailure(reason) =>
      subscriptionBus.writer.onNext(
        PlanSubscription.Message.Error(reason)
      )
  }
}
