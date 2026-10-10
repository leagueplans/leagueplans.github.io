package com.leagueplans.ui.dom.planning.forest

import com.leagueplans.ui.dom.planning.forest.ForestUpdateConsumer.LazyNode
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.common.forest.Forest.Update.*
import com.raquo.airstream.core.Signal
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.OptionValues
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers

import scala.collection.mutable
import scala.util.Using

final class ForestUpdateConsumerTest extends AnyFreeSpec with Matchers with OptionValues {
  private final case class TestNode(
    id: String,
    data: Signal[Int],
    parent: Signal[Option[TestNode]],
    children: Signal[List[LazyNode[TestNode]]]
  )

  /** a has children b and c. b has child d. e is another root. */
  private val forest = Forest.from(
    nodes = Map("a" -> 1, "b" -> 2, "c" -> 3, "d" -> 4, "e" -> 5),
    parentsToChildren = Map("a" -> List("b", "c"), "b" -> List("d")),
    roots = List("a", "e")
  )

  /** Returns the consumer, along with the IDs of the nodes that it has created, in order */
  private def build(forest: Forest[String, Int]): (ForestUpdateConsumer[String, Int, TestNode], mutable.Buffer[String]) = {
    val created = mutable.Buffer.empty[String]
    val consumer = ForestUpdateConsumer[String, Int, TestNode](
      forest,
      (id, data, parent, children) => {
        created += id
        TestNode(id, data, parent, children)
      }
    )
    (consumer, created)
  }

  private def observed[A](signal: Signal[A])(using owner: ManualOwner): A =
    signal.observe(using owner).now()

  "ForestUpdateConsumer" - {
    "should not create any nodes until they are asked for" in {
      val (_, created) = build(forest)
      created shouldBe empty
    }

    "should create a node when it is asked for" in {
      val (consumer, created) = build(forest)
      consumer.get("a").map(_.id) shouldEqual Some("a")
      created.toList shouldEqual List("a")
    }

    "should create a node once, however often it is asked for" in {
      val (consumer, created) = build(forest)
      val first = consumer.get("a")
      consumer.get("a") shouldEqual first
      created.toList shouldEqual List("a")
    }

    "should not have anything to give for unknown IDs" in {
      val (consumer, created) = build(forest)
      consumer.get("z") shouldEqual None
      created shouldBe empty
    }

    "should not create a node's children when its own children are read" in {
      Using(new ManualOwner) { owner =>
        val (consumer, created) = build(forest)
        val children = observed(consumer.get("a").value.children)(using owner)
        children.map(_.isCreated) shouldEqual List(false, false)
        created.toList shouldEqual List("a")
      }(using _.killSubscriptions()).get
    }

    "should create a child when it is taken from its parent's children" in {
      Using(new ManualOwner) { owner =>
        val (consumer, created) = build(forest)
        val children = observed(consumer.get("a").value.children)(using owner)
        children.map(_.node.id) shouldEqual List("b", "c")
        created.toList shouldEqual List("a", "b", "c")
        consumer.get("b") shouldEqual Some(children.head.node)
      }(using _.killSubscriptions()).get
    }

    "should only create a node's parent once something reads it" in {
      Using(new ManualOwner) { owner =>
        val (consumer, created) = build(forest)
        val node = consumer.get("d").value
        created.toList shouldEqual List("d")
        observed(node.parent)(using owner).map(_.id) shouldEqual Some("b")
        created.toList shouldEqual List("d", "b")
      }(using _.killSubscriptions()).get
    }

    "should apply updates to nodes that have not been created" in {
      Using(new ManualOwner) { owner =>
        val (consumer, created) = build(forest)
        consumer.eval(List(
          UpdateData("d", 40),
          AddNode("f", 6),
          AddLink("f", "b"),
          RemoveLink("c", "a"),
          AddLink("c", "e"),
          Reorder(List("f", "d"), Some("b"))
        ))
        created shouldBe empty

        val d = consumer.get("d").value
        observed(d.data)(using owner) shouldEqual 40
        val b = consumer.get("b").value
        observed(b.children)(using owner).map(_.node.id) shouldEqual List("f", "d")
        observed(consumer.get("f").value.parent)(using owner).map(_.id) shouldEqual Some("b")
        observed(consumer.get("a").value.children)(using owner).map(_.node.id) shouldEqual List("b")
        observed(consumer.get("e").value.children)(using owner).map(_.node.id) shouldEqual List("c")
        observed(consumer.get("c").value.parent)(using owner).map(_.id) shouldEqual Some("e")
      }(using _.killSubscriptions()).get
    }

    "should not create removed nodes" in {
      val (consumer, created) = build(forest)
      consumer.eval(List(RemoveNode("e")))
      consumer.get("e") shouldEqual None
      created shouldBe empty
    }

    "should announce nodes being added or removed" in {
      Using(new ManualOwner) { owner =>
        val (consumer, _) = build(forest)
        var announcements = 0
        consumer.nodeChanges.foreach(_ => announcements += 1)(using owner)
        consumer.eval(List(AddNode("f", 6), AddNode("g", 7), UpdateData("a", 10)))
        announcements shouldEqual 1
        consumer.eval(List(UpdateData("a", 11)))
        announcements shouldEqual 1
      }(using _.killSubscriptions()).get
    }
  }
}
