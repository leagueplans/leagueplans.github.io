package ddm.ui.storage.client

import com.raquo.airstream.eventbus.EventBus
import ddm.ui.facades.workers.{CreateWorkerOptions, WorkerFactory}
import ddm.ui.model.plan.Plan
import ddm.ui.storage.model.errors.{DeletionError, FileSystemError}
import ddm.ui.storage.model.{PlanID, PlanMetadata}
import ddm.ui.storage.worker.StorageProtocol.{Inbound, Outbound}
import ddm.ui.storage.worker.{StorageCoordinator, StorageProtocol}
import ddm.ui.utils.circe.OrCodec.{orDecoder, orEncoder}
import ddm.ui.wrappers.workers.MessagePortClient
import io.circe.{Decoder, Encoder}

import java.util.UUID
import scala.collection.mutable
import scala.concurrent.Promise

object StorageClient {
  def apply(): StorageClient = {
    val coordinator = startCoordinator()
    val worker =
      MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator](
        WorkerFactory.storageWorker(CreateWorkerOptions.storageWorker)
      )
      
    worker.setMessageHandler(coordinator.send)

    new StorageClient(coordinator.send, toSetMessageHandler(coordinator, worker))
  }
  
  private def startCoordinator(): MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut] = {
    given Encoder[StorageCoordinator.MsgIn] = orEncoder[Inbound.ToCoordinator, Outbound.ToCoordinator]
    given Decoder[StorageCoordinator.MsgOut] = orDecoder[Outbound.ToClient, Inbound.ToWorker]
    
    MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut](
      WorkerFactory.storageCoordinator(CreateWorkerOptions.storageCoordinator)
    )
  }
  
  private def toSetMessageHandler(
    coordinator: MessagePortClient[StorageCoordinator.MsgIn, StorageCoordinator.MsgOut],
    worker: MessagePortClient[Inbound.ToWorker, Outbound.ToCoordinator]
  ): (Outbound.ToClient => ?) => Unit =
    onResponse => coordinator.setMessageHandler {
      case message: Inbound.ToWorker => worker.send(message)
      case message: Outbound.ToClient => onResponse(message)
    }
}

final class StorageClient(
  send: Inbound.ToCoordinator => Unit,
  setMessageHandler: (Outbound.ToClient => ?) => Unit
) {
  private object requests {
    val listPlans: mutable.Set[Promise[Either[FileSystemError, Map[PlanID, PlanMetadata]]]] =
      mutable.Set.empty

    val creates: mutable.Map[String, Promise[Either[FileSystemError, PlanID]]] =
      mutable.Map.empty

    val deletes: mutable.Map[PlanID, List[Promise[Either[DeletionError, Unit]]]] =
      mutable.Map.empty

    val subscriptions: mutable.Map[PlanID, List[Promise[Either[FileSystemError, (Plan, PlanSubscription)]]]] =
      mutable.Map.empty
  }

  private val subscriptionBus = EventBus[(PlanID, PlanSubscription.Message) | PlanSubscription.Message]()

  def listPlans(): Promise[Either[FileSystemError, Map[PlanID, PlanMetadata]]] = {
    val promise = Promise[Either[FileSystemError, Map[PlanID, PlanMetadata]]]()
    requests.listPlans += promise
    send(Inbound.ListPlans)
    promise
  }

  def create(name: String, plan: Plan): Promise[Either[FileSystemError, PlanID]] = {
    val requestID = UUID.randomUUID().toString
    val promise = Promise[Either[FileSystemError, PlanID]]()
    requests.creates += requestID -> promise
    send(Inbound.Create(requestID, name, plan))
    promise
  }

  def delete(id: PlanID): Promise[Either[DeletionError, Unit]] = {
    val promise = Promise[Either[DeletionError, Unit]]()
    requests.deletes += id -> (requests.deletes.getOrElse(id, List.empty) :+ promise)
    send(Inbound.Delete(id))
    promise
  }

  def subscribe(id: PlanID): Promise[Either[FileSystemError, (Plan, PlanSubscription)]] = {
    val promise = Promise[Either[FileSystemError, (Plan, PlanSubscription)]]()
    requests.subscriptions += id -> (requests.subscriptions.getOrElse(id, List.empty) :+ promise)
    send(Inbound.Subscribe(id))
    promise
  }

  setMessageHandler {
    case Outbound.Plans(data) =>
      requests.listPlans.foreach { promise =>
        promise.success(Right(data))
        requests.listPlans -= promise
      }

    case Outbound.ListPlansFailed(reason) =>
      requests.listPlans.foreach { promise =>
        promise.success(Left(reason))
        requests.listPlans -= promise
      }

    case Outbound.CreateSucceeded(requestID, planID) =>
      requests.creates.get(requestID).foreach { promise =>
        promise.success(Right(planID))
        requests.creates -= requestID
      }

    case Outbound.CreateFailed(requestID, reason) =>
      requests.creates.get(requestID).foreach { promise =>
        promise.success(Left(reason))
        requests.creates -= requestID
      }

    case Outbound.Subscription(planID, lamport, plan) =>
      requests.subscriptions.get(planID).foreach { promises =>
        promises.foreach { promise =>
          val subscription = new PlanSubscription(
            lamport,
            subscriptionBus.events.collect {
              case (`planID`, message) => message
              case message: PlanSubscription.Message => message
            },
            save = (lamport, update) => send(Inbound.Update(planID, lamport, update)),
            unsubscribe = () => send(Inbound.Unsubscribe(planID))
          )
          promise.success(Right((plan, subscription)))
        }
        requests.subscriptions -= planID
      }

    case Outbound.SubscriptionFailed(planID, reason) =>
      requests.subscriptions.get(planID).foreach { promises =>
        promises.foreach(_.success(Left(reason)))
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
      requests.deletes.get(planID).foreach { promises =>
        promises.foreach(_.success(Right(())))
        requests.deletes -= planID
      }

    case Outbound.DeleteFailed(planID, reason) =>
      requests.deletes.get(planID).foreach { promises =>
        promises.foreach(_.success(Left(reason)))
        requests.deletes -= planID
      }

    case Outbound.ProtocolFailure(reason) =>
      subscriptionBus.writer.onNext(
        PlanSubscription.Message.Error(reason)
      )
  }
}
