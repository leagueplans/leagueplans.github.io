package com.leagueplans.ui.storage.worker

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.storage.model.errors.SubscriptionError
import com.leagueplans.ui.storage.model.{LamportTimestamp, PlanID, PlanList, Rename, StepUpdates}
import com.leagueplans.ui.storage.worker.StorageProtocol.{ToClient, ToCoordinator, WireUpdate}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

final class StorageProtocolTest extends AnyFreeSpec with Matchers {
  private val planID = PlanID.fromString("plan")
  private val lamport = LamportTimestamp.initial.increment

  private def roundTrip[T : Encoder : Decoder](message: T): T =
    Decoder.decodeMessage[T](message.encoded.getBytes).left.map(throw _).merge

  "StorageProtocol" - {
    "round-trips cancellations" in {
      val message: ToCoordinator = ToCoordinator.CancelSubscribe(7)
      roundTrip(message) shouldEqual message
    }

    "round-trips plans that couldn't be read" in {
      val message: ToClient = ToClient.Plans(7, PlanList(Map.empty, Set(planID)))
      roundTrip(message) shouldEqual message
    }

    "round-trips progress" in {
      val message: ToClient = ToClient.SubscribeProgress(7, 12, 3400)
      roundTrip(message) shouldEqual message
    }

    "round-trips cancelled subscriptions" in {
      val message: ToClient = ToClient.SubscriptionFailed(7, planID, SubscriptionError.Cancelled)
      roundTrip(message) shouldEqual message
    }

    "round-trips each kind of update, in both directions" in {
      val updates = List[StepUpdates | com.leagueplans.ui.model.plan.Plan.Settings | Rename](
        StepUpdates(List.empty),
        Armageddon.settings,
        Rename("A new name")
      )

      updates.foreach { update =>
        val toCoordinator: ToCoordinator = ToCoordinator.Update(planID, lamport, update)
        roundTrip(toCoordinator) shouldEqual toCoordinator

        val toClient: ToClient = ToClient.Update(planID, lamport, WireUpdate(update))
        roundTrip(toClient) shouldEqual toClient
        toClient match {
          case ToClient.Update(_, _, wire) => wire.toUnion shouldEqual update
          case _ => fail()
        }
      }
    }
  }
}
