package com.leagueplans.ui.storage.worker

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.storage.model.errors.{DeletionError, FileSystemError, SubscriptionError, UpdateError}
import com.leagueplans.ui.storage.model.{LamportTimestamp, PlanExport, PlanID, PlanMetadata, Rename, StepUpdates}

object StorageProtocol {
  /** [[StepUpdates]], [[Plan.Settings]] and [[Rename]] as a type that the codec can derive */
  enum WireUpdate {
    case Steps(updates: StepUpdates)
    case Settings(settings: Plan.Settings)
    case Name(rename: Rename)

    def toUnion: StepUpdates | Plan.Settings | Rename =
      this match {
        case Steps(updates) => updates
        case Settings(settings) => settings
        case Name(rename) => rename
      }
  }

  object WireUpdate {
    def apply(update: StepUpdates | Plan.Settings | Rename): WireUpdate =
      update match {
        case updates: StepUpdates => Steps(updates)
        case settings: Plan.Settings => Settings(settings)
        case rename: Rename => Name(rename)
      }

    given Encoder[WireUpdate] = Encoder.derived
    given Decoder[WireUpdate] = Decoder.derived
  }

  sealed trait ToCoordinator

  object ToCoordinator {
    final case class ListPlans(requestID: Long) extends ToCoordinator

    final case class Create(
      requestID: Long,
      metadata: PlanMetadata,
      plan: Plan
    ) extends ToCoordinator

    final case class Fetch(requestID: Long, planID: PlanID) extends ToCoordinator

    object Update {
      def apply(
        planID: PlanID,
        lamport: LamportTimestamp,
        update: StepUpdates | Plan.Settings | Rename
      ): Update =
        Update(planID, lamport, WireUpdate(update))
    }

    final case class Update(
      planID: PlanID,
      lamport: LamportTimestamp,
      update: WireUpdate
    ) extends ToCoordinator

    final case class Delete(requestID: Long, planID: PlanID) extends ToCoordinator

    final case class Subscribe(requestID: Long, planID: PlanID) extends ToCoordinator

    /** Asks the coordinator to abandon a [[Subscribe]]. This is handled as soon as it arrives,
      * rather than in turn, so that a slow read doesn't delay it. The subscribe request still gets a
      * reply: [[ToClient.SubscriptionFailed]] with [[SubscriptionError.Cancelled]] if the
      * cancellation was in time, or the outcome of the read if it wasn't. */
    final case class CancelSubscribe(requestID: Long) extends ToCoordinator

    final case class Unsubscribe(requestID: Long, planID: PlanID) extends ToCoordinator

    given Encoder[ToCoordinator] = Encoder.derived
    given Decoder[ToCoordinator] = Decoder.derived
  }

  sealed trait ToClient

  object ToClient {
    final case class Plans(requestID: Long, data: Map[PlanID, PlanMetadata]) extends ToClient
    final case class ListPlansFailed(requestID: Long, reason: FileSystemError) extends ToClient

    final case class CreateSucceeded(requestID: Long, planID: PlanID) extends ToClient
    final case class CreateFailed(requestID: Long, reason: FileSystemError) extends ToClient

    final case class FetchSucceeded(requestID: Long, planID: PlanID, plan: PlanExport) extends ToClient
    final case class FetchFailed(requestID: Long, planID: PlanID, reason: FileSystemError) extends ToClient

    final case class Subscription(requestID: Long, planID: PlanID, lamport: LamportTimestamp, plan: Plan) extends ToClient
    /** Sent while a subscription's plan is being read, until the reply to the request is sent */
    final case class SubscribeProgress(requestID: Long, stepsRead: Int, totalSteps: Int) extends ToClient
    final case class SubscriptionFailed(requestID: Long, planID: PlanID, reason: SubscriptionError) extends ToClient
    final case class SubscriptionTerminated(planID: PlanID) extends ToClient
    /** The plan was opened by another version of the app, which now owns it */
    final case class SubscriptionTakenOver(planID: PlanID) extends ToClient

    final case class Update(
      planID: PlanID,
      lamport: LamportTimestamp,
      update: WireUpdate
    ) extends ToClient

    final case class UpdateSucceeded(
      planID: PlanID,
      lamport: LamportTimestamp
    ) extends ToClient

    final case class UpdateFailed(
      planID: PlanID,
      lamport: LamportTimestamp,
      reason: UpdateError
    ) extends ToClient

    final case class DeleteSucceeded(requestID: Long, planID: PlanID) extends ToClient
    final case class DeleteFailed(requestID: Long, planID: PlanID, reason: DeletionError) extends ToClient

    given Encoder[ToClient] = Encoder.derived
    given Decoder[ToClient] = Decoder.derived
  }
}
