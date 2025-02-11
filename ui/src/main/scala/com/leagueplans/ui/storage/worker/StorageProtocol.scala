package com.leagueplans.ui.storage.worker

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.storage.model.errors.{DeletionError, FileSystemError, ProtocolError, UpdateError}
import com.leagueplans.ui.storage.model.{LamportTimestamp, PlanExport, PlanID, PlanMetadata}

object StorageProtocol {
  object Inbound {
    sealed trait ToCoordinator
    sealed trait ToWorker

    case object ListPlans extends ToCoordinator with ToWorker

    final case class Create(
      requestID: String,
      metadata: PlanMetadata,
      plan: Plan
    ) extends ToCoordinator with ToWorker
    
    final case class Read(planID: PlanID) extends ToWorker
    
    final case class Fetch(planID: PlanID) extends ToCoordinator with ToWorker

    object Update {
      def apply(
        planID: PlanID,
        lamport: LamportTimestamp,
        update: Plan.Settings | Forest.Update[Step.ID, Step]
      ): Update =
        Update(
          planID,
          lamport,
          update match {
            case settings: Plan.Settings => Left(settings)
            case fu: Forest.Update[Step.ID, Step] => Right(fu)
          }
        )
    }

    final case class Update(
      planID: PlanID,
      lamport: LamportTimestamp,
      update: Either[Plan.Settings, Forest.Update[Step.ID, Step]]
    ) extends ToCoordinator with ToWorker

    final case class Delete(planID: PlanID) extends ToCoordinator with ToWorker

    final case class Subscribe(planID: PlanID) extends ToCoordinator

    final case class Unsubscribe(planID: PlanID) extends ToCoordinator

    object ToCoordinator {
      given Encoder[ToCoordinator] = Encoder.derived
      given Decoder[ToCoordinator] = Decoder.derived
    }

    object ToWorker {
      given Encoder[ToWorker] = Encoder.derived
      given Decoder[ToWorker] = Decoder.derived
    }
  }

  object Outbound {
    sealed trait ToClient
    sealed trait ToCoordinator
    
    final case class Plans(data: Map[PlanID, PlanMetadata]) extends ToClient with ToCoordinator
    final case class ListPlansFailed(reason: FileSystemError) extends ToClient with ToCoordinator
 
    final case class CreateSucceeded(requestID: String, planID: PlanID) extends ToClient with ToCoordinator
    final case class CreateFailed(requestID: String, reason: FileSystemError) extends ToClient with ToCoordinator

    final case class FetchSucceeded(planID: PlanID, plan: PlanExport) extends ToClient with ToCoordinator
    final case class FetchFailed(planID: PlanID, reason: FileSystemError) extends ToClient with ToCoordinator
 
    final case class Subscription(planID: PlanID, lamport: LamportTimestamp, plan: Plan) extends ToClient
    final case class SubscriptionFailed(planID: PlanID, reason: FileSystemError) extends ToClient
    final case class SubscriptionTerminated(planID: PlanID) extends ToClient
    
    final case class ReadSucceeded(planID: PlanID, plan: Plan) extends ToCoordinator
    final case class ReadFailed(planID: PlanID, reason: FileSystemError) extends ToCoordinator
 
    final case class Update(
      planID: PlanID,
      lamport: LamportTimestamp,
      update: Either[Plan.Settings, Forest.Update[Step.ID, Step]]
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
    final case class ProtocolFailure(reason: ProtocolError) extends ToClient

    object ToClient {
      given Encoder[ToClient] = Encoder.derived
      given Decoder[ToClient] = Decoder.derived
    }

    object ToCoordinator {
      given Encoder[ToCoordinator] = Encoder.derived
      given Decoder[ToCoordinator] = Decoder.derived
    }
  }
}
