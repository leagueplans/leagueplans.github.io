package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.dom.planning.forest.ForestUpdateConsumer
import com.leagueplans.ui.model.common.forest.ForestPropertyTest.{TreeNode, allIDs}
import com.leagueplans.ui.model.plan.Step
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.raquo.airstream.ownership.ManualOwner
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers

import scala.util.Using

/** Checks that consumers of forest updates agree with a forest */
trait ForestAssertions extends Matchers {
  /** Every step in the forest has a node in the tree, whose data, parent and children match */
  def treeMatches(
    tree: ForestUpdateConsumer[Step.ID, Step, TreeNode],
    forest: Forest[Step.ID, Step]
  ): Assertion =
    Using(new ManualOwner) { owner =>
      allIDs.filterNot(forest.contains).flatMap(tree.get) shouldBe empty
      forest.nodes.foreach { (id, step) =>
        val node = tree.get(id).getOrElse(fail(s"No node for $id"))
        withClue(s"Node $id:") {
          node.data.observe(using owner).now() shouldEqual step
          node.parent.observe(using owner).now().map(_.id) shouldEqual forest.toParent.get(id)
          node.children.observe(using owner).now().map(_.id) shouldEqual forest.toChildren(id)
        }
      }
      succeed
    }(using _.killSubscriptions()).get

  def timingsMatch(
    incremental: TimeKeeper,
    fromScratch: TimeKeeper,
    forest: Forest[Step.ID, Step]
  ): Assertion = {
    def timings(timeKeeper: TimeKeeper) =
      forest.nodes.keys.map(id => id -> timeKeeper.get(id).now()).toMap

    timings(incremental) shouldEqual timings(fromScratch)
    incremental.endTime.now() shouldEqual fromScratch.endTime.now()
  }
}
