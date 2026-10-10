package com.leagueplans.ui.storage.client

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.model.status.StatusTracker.Status
import com.leagueplans.ui.storage.model.errors.SubscriptionError
import com.leagueplans.ui.storage.model.{LamportTimestamp, LoadProgress, PlanID}
import com.leagueplans.ui.storage.worker.StorageProtocol.{ToClient, ToCoordinator}
import com.leagueplans.uicommon.wrappers.workers.MessagePortClient
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable

final class StorageClientTest extends AnyFreeSpec with Matchers {
  private val planID = PlanID.fromString("plan")
  private val plan = Plan("Plan", Forest.empty, Armageddon.settings)

  private final class FakeCoordinator extends MessagePortClient[ToCoordinator, ToClient] {
    val sent = mutable.ListBuffer.empty[ToCoordinator]
    private var handler: ToClient => ? = _ => ()

    def setMessageHandler(f: ToClient => ?): Unit = handler = f
    def send(message: ToCoordinator): Unit = sent += message
    def close(): Unit = ()

    def reply(message: ToClient): Unit = handler(message): Unit

    def subscribeIDs: List[Long] =
      sent.toList.collect { case ToCoordinator.Subscribe(id, _) => id }
  }

  private def test(f: (StorageClient, FakeCoordinator) => Any): Unit = {
    val coordinator = FakeCoordinator()
    f(new StorageClient(coordinator), coordinator): Unit
  }

  "StorageClient" - {
    "subscribe" - {
      "reports progress as the coordinator sends it" in test { (client, coordinator) =>
        val owner = new ManualOwner
        val pending = client.subscribe(planID)
        val id = coordinator.subscribeIDs.head
        val progress = pending.progress.observe(using owner)
        progress.now() shouldBe None

        coordinator.reply(ToClient.SubscribeProgress(id, 0, 40))
        progress.now() shouldBe Some(LoadProgress(0, 40))

        coordinator.reply(ToClient.SubscribeProgress(id, 25, 40))
        progress.now() shouldBe Some(LoadProgress(25, 40))
        owner.killSubscriptions()
      }

      "cancelling resolves the result immediately and tells the coordinator" in test { (client, coordinator) =>
        val pending = client.subscribe(planID)
        val id = coordinator.subscribeIDs.head
        pending.result.now() shouldBe None

        pending.cancel()
        pending.result.now() shouldEqual Some(Left(SubscriptionError.Cancelled))
        coordinator.sent.toList.last shouldEqual ToCoordinator.CancelSubscribe(id)
      }

      "cancelling leaves the client idle without failing" in test { (client, coordinator) =>
        val owner = new ManualOwner
        val status = client.status.observe(using owner)
        val pending = client.subscribe(planID)
        val id = coordinator.subscribeIDs.head
        status.now() shouldEqual Status.Busy

        pending.cancel()
        status.now() shouldEqual Status.Idle

        coordinator.reply(ToClient.SubscriptionFailed(id, planID, SubscriptionError.Cancelled))
        status.now() shouldEqual Status.Idle
        owner.killSubscriptions()
      }

      "ignores progress after cancelling" in test { (client, coordinator) =>
        val pending = client.subscribe(planID)
        pending.cancel()
        coordinator.reply(ToClient.SubscribeProgress(coordinator.subscribeIDs.head, 1, 2))
        pending.progress.now() shouldBe None
      }

      "closes, and doesn't expose, a subscription that arrives after cancelling" in test { (client, coordinator) =>
        val pending = client.subscribe(planID)
        pending.cancel()
        val sentBefore = coordinator.sent.size

        coordinator.reply(ToClient.Subscription(coordinator.subscribeIDs.head, planID, LamportTimestamp.initial, plan))
        pending.result.now() shouldEqual Some(Left(SubscriptionError.Cancelled))
        coordinator.sent.drop(sentBefore).toList should matchPattern {
          case List(ToCoordinator.Unsubscribe(_, `planID`)) =>
        }
      }

      "keeps the plan subscribed if it's been asked for again since cancelling" in test { (client, coordinator) =>
        val cancelled = client.subscribe(planID)
        cancelled.cancel()
        val again = client.subscribe(planID)
        val sentBefore = coordinator.sent.size

        coordinator.reply(ToClient.Subscription(coordinator.subscribeIDs.head, planID, LamportTimestamp.initial, plan))
        coordinator.sent.drop(sentBefore) shouldBe empty

        coordinator.reply(ToClient.Subscription(coordinator.subscribeIDs.last, planID, LamportTimestamp.initial, plan))
        again.result.now() should matchPattern { case Some(Right((`plan`, _))) => }
      }

      "doesn't treat other requests as cancelled" in test { (client, coordinator) =>
        val cancelled = client.subscribe(planID)
        val kept = client.subscribe(planID)
        cancelled.cancel()

        coordinator.reply(ToClient.Subscription(coordinator.subscribeIDs.last, planID, LamportTimestamp.initial, plan))
        kept.result.now() should matchPattern { case Some(Right((`plan`, _))) => }
      }

      "ignores cancelling once the plan has arrived" in test { (client, coordinator) =>
        val pending = client.subscribe(planID)
        coordinator.reply(ToClient.Subscription(coordinator.subscribeIDs.head, planID, LamportTimestamp.initial, plan))
        val sentBefore = coordinator.sent.size

        pending.cancel()
        pending.result.now() should matchPattern { case Some(Right(_)) => }
        coordinator.sent.size shouldEqual sentBefore
      }

      "reports failures other than cancellation" in test { (client, coordinator) =>
        val owner = new ManualOwner
        val status = client.status.observe(using owner)
        val pending = client.subscribe(planID)
        val error = SubscriptionError.LockUnavailable("nope")

        coordinator.reply(ToClient.SubscriptionFailed(coordinator.subscribeIDs.head, planID, error))
        pending.result.now() shouldEqual Some(Left(error))
        status.now() shouldEqual Status.Failed(error.message)
        owner.killSubscriptions()
      }
    }
  }
}
