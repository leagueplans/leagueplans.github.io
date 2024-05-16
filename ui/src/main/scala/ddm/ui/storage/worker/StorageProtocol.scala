package ddm.ui.storage.worker

import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.{Plan, Step}
import ddm.ui.storage.model.errors.{DeletionError, FileSystemError, UpdateError}
import ddm.ui.storage.model.{LamportTimestamp, PlanID, PlanMetadata}
import ddm.ui.utils.circe.FiniteDurationCodec.{decoder, encoder}
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

import scala.concurrent.duration.FiniteDuration

object StorageProtocol {
  //TODO Importing a plan with an older schema version?
  //TODO Exporting a plan - support arbitrary JSON to allow fixing schema mistakes
  object Inbound {
    sealed trait ToCoordinator
    sealed trait ToWorker

    case object ListPlans extends ToCoordinator with ToWorker

    final case class Create(
      requestID: String,
      name: String,
      plan: Plan
    ) extends ToCoordinator with ToWorker
    
    final case class Read(planID: PlanID) extends ToWorker
    
    final case class Update(
      planID: PlanID,
      lamport: LamportTimestamp,
      update: Forest.Update[Step.ID, Step]
    ) extends ToCoordinator with ToWorker

    final case class Delete(planID: PlanID) extends ToCoordinator with ToWorker

    final case class Subscribe(planID: PlanID) extends ToCoordinator

    final case class Unsubscribe(planID: PlanID) extends ToCoordinator

    object ToCoordinator {
      given Codec[ToCoordinator] = deriveCodec[ToCoordinator]
    }

    object ToWorker {
      given Codec[ToWorker] = deriveCodec[ToWorker]
    }
  }

  object Outbound {
    sealed trait ToClient
    sealed trait ToCoordinator
    
    final case class Plans(data: Map[PlanID, PlanMetadata]) extends ToClient with ToCoordinator
    final case class ListPlansFailed(reason: FileSystemError) extends ToClient with ToCoordinator
 
    final case class CreateSucceeded(requestID: String, planID: PlanID) extends ToClient with ToCoordinator
    final case class CreateFailed(requestID: String, reason: FileSystemError) extends ToClient with ToCoordinator
 
    final case class Subscription(planID: PlanID, lamport: LamportTimestamp, plan: Plan) extends ToClient
    final case class SubscriptionFailed(planID: PlanID, reason: FileSystemError) extends ToClient
    final case class SubscriptionTerminated(planID: PlanID) extends ToClient
    
    final case class ReadSucceeded(planID: PlanID, plan: Plan) extends ToCoordinator
    final case class ReadFailed(planID: PlanID, reason: FileSystemError) extends ToCoordinator
 
    final case class Update(
      planID: PlanID,
      lamport: LamportTimestamp,
      update: Forest.Update[Step.ID, Step]
    ) extends ToClient
    
    final case class UpdateSucceeded(
      planID: PlanID,
      lamport: LamportTimestamp
    ) extends ToClient with ToCoordinator
    
    final case class UpdateFailed(
      planID: PlanID,
      lamport: LamportTimestamp, 
      reason: UpdateError
    ) extends ToClient with ToCoordinator
 
    final case class DeleteSucceeded(planID: PlanID) extends ToClient with ToCoordinator
    final case class DeleteFailed(planID: PlanID, reason: DeletionError) extends ToClient with ToCoordinator
 
    final case class WorkerFailedToStart(error: FileSystemError) extends ToCoordinator
    final case class ProtocolError(cause: ToCoordinator) extends ToClient with ToCoordinator
    // This exists for the case where the user closes their browser window (thereby killing the worker)
    // while the coordinator is waiting for a response from the worker
    final case class WorkerFailedToRespond(duration: FiniteDuration) extends ToClient

    object ToClient {
      given Codec[ToClient] = deriveCodec[ToClient]
    }

    object ToCoordinator {
      given Codec[ToCoordinator] = deriveCodec[ToCoordinator]
    }
  }
}