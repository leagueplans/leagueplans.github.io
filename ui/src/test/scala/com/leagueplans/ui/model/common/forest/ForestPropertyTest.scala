package com.leagueplans.ui.model.common.forest

import com.leagueplans.ui.dom.planning.forest.ForestUpdateConsumer
import com.leagueplans.ui.model.common.forest.Forest.Update
import com.leagueplans.ui.model.common.forest.ForestPropertyTest.*
import com.leagueplans.ui.model.plan.{Duration, Plan, Step, StepDetails}
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.projection.calculation.TimeKeeper
import com.leagueplans.ui.storage.model.{PlanMetadata, StepUpdates}
import com.leagueplans.ui.storage.opfs.PlanDirectory
import com.leagueplans.uicommon.utils.airstream.ObservableOps.flatMapConcat
import com.leagueplans.uicommon.wrappers.opfs.{FileSystemError, MockDirectoryHandle}
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.airstream.ownership.ManualOwner
import org.scalacheck.Gen
import org.scalatest.Assertion
import org.scalatest.freespec.AnyFreeSpec
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import scala.util.{Random, Using}

/** Applies random sequences of operations through [[ForestInterpreter]] and [[ForestResolver]],
  * as [[com.leagueplans.ui.dom.planning.forest.Forester]] does. */
final class ForestPropertyTest
  extends AnyFreeSpec
    with ForestAssertions
    with ScalaCheckDrivenPropertyChecks {

  override implicit val generatorDrivenConfig: PropertyCheckConfiguration =
    PropertyCheckConfiguration(minSuccessful = 200)

  /** Applies each operation in turn, checking every transition */
  private def checkEachOp(ops: List[Op])(
    check: (Forest[Step.ID, Step], Op, List[Update[Step.ID, Step]], Forest[Step.ID, Step]) => Assertion
  ): Assertion = {
    ops.foldLeft(Forest.empty[Step.ID, Step]) { (forest, op) =>
      val updates = toUpdates(forest, op)
      val updated = ForestResolver.resolve(forest, updates)
      withClue(s"Applying $op to $forest, producing $updates:")(check(forest, op, updates, updated))
      updated
    }
    succeed
  }

  "Forest operations" - {
    "leave the forest well-formed" in forAll(opsGen)(checkEachOp(_) { (_, _, _, updated) =>
      Forest.validated(updated.nodes, updated.toChildren, updated.roots) shouldEqual Right(updated)
    })

    "have the documented effect on the forest" in forAll(opsGen)(checkEachOp(_) { (forest, op, _, updated) =>
      updated shouldEqual expected(forest, op)
    })

    "only produce updates when the forest changes" in forAll(opsGen)(checkEachOp(_) { (forest, _, updates, updated) =>
      updates.isEmpty shouldEqual (updated == forest)
    })

    "are mirrored by the step tree" in forAll(opsGen) { ops =>
      val tree = ForestUpdateConsumer[Step.ID, Step, TreeNode](Forest.empty, TreeNode(_, _, _, _))
      checkEachOp(ops) { (_, _, updates, updated) =>
        tree.eval(updates)
        treeMatches(tree, updated)
      }
    }

    "are mirrored by a step tree built from their result" in forAll(opsGen) { ops =>
      val forest = resolveAll(ops)
      treeMatches(ForestUpdateConsumer[Step.ID, Step, TreeNode](forest, TreeNode(_, _, _, _)), forest)
    }

    "keep step timings the same as working them out from scratch" in forAll(opsGen) { ops =>
      val timeKeeper = TimeKeeper(Forest.empty)
      checkEachOp(ops) { (_, _, updates, updated) =>
        updates.foreach(timeKeeper.update)
        timingsMatch(timeKeeper, TimeKeeper(updated), updated)
      }
    }

    "are persisted faithfully" in forAll(opsGen) { ops =>
      // Like the Forester, each operation's updates are persisted as one batch
      val (forest, batches) =
        ops.foldLeft((Forest.empty[Step.ID, Step], Vector.empty[List[Update[Step.ID, Step]]])) {
          case ((forest, batches), op) =>
            val updates = toUpdates(forest, op)
            (ForestResolver.resolve(forest, updates), if (updates.isEmpty) batches else batches :+ updates)
        }

      persist(batches) shouldEqual forest
    }

    "are persisted faithfully when combined into a single batch" in forAll(opsGen) { ops =>
      val (forest, updates) =
        ops.foldLeft((Forest.empty[Step.ID, Step], List.empty[Update[Step.ID, Step]])) {
          case ((forest, allUpdates), op) =>
            val updates = toUpdates(forest, op)
            (ForestResolver.resolve(forest, updates), allUpdates ++ updates)
        }

      persist(List(updates)) shouldEqual forest
    }
  }
}

private object ForestPropertyTest {
  /** What the step tree gives each node it creates */
  final case class TreeNode(
    id: Step.ID,
    data: Signal[Step],
    parent: Signal[Option[TreeNode]],
    children: Signal[List[TreeNode]]
  )

  enum Corruption {
    case Valid, Missing, Duplicated
  }

  enum Op {
    case Add(id: Step.ID, parent: Option[Step.ID], variant: Variant)
    case Move(id: Step.ID, parent: Option[Step.ID])
    case Remove(id: Step.ID)
    case Edit(id: Step.ID, variant: Variant)
    case Reorder(anchor: Step.ID, seed: Long, corruption: Corruption)
  }

  // A small pool, so that operations regularly target steps that exist, steps that
  // don't, and steps that have been removed and re-added
  val allIDs: List[Step.ID] =
    List.tabulate(12)(i => Step.ID.fromString(s"step-$i"))

  private val idGen: Gen[Step.ID] =
    Gen.oneOf(allIDs)

  /** Kinds of step, so that timings vary. A step with no duration takes the time of its
    * substeps, and a step with no repetitions takes no time at all. */
  enum Variant(val repetitions: Int, val duration: Duration) {
    case Untimed extends Variant(repetitions = 1, Duration.ticks(0))
    case Timed extends Variant(repetitions = 1, Duration.ticks(5))
    case Looped extends Variant(repetitions = 2, Duration.ticks(3))
    case Skipped extends Variant(repetitions = 0, Duration.ticks(4))
  }

  private val variantGen: Gen[Variant] =
    Gen.oneOf(Variant.values.toSeq)

  private val opGen: Gen[Op] =
    Gen.frequency(
      4 -> Gen.zip(idGen, Gen.option(idGen), variantGen).map(Op.Add(_, _, _)),
      3 -> Gen.zip(idGen, Gen.option(idGen)).map(Op.Move(_, _)),
      2 -> idGen.map(Op.Remove(_)),
      2 -> Gen.zip(idGen, variantGen).map(Op.Edit(_, _)),
      3 -> Gen.zip(
        idGen,
        Gen.long,
        Gen.frequency(4 -> Corruption.Valid, 1 -> Corruption.Missing, 1 -> Corruption.Duplicated)
      ).map(Op.Reorder(_, _, _))
    )

  val opsGen: Gen[List[Op]] =
    Gen.choose(0, 40).flatMap(Gen.listOfN(_, opGen))

  private def step(id: Step.ID, variant: Variant): Step =
    Step(id, StepDetails(variant.toString).copy(repetitions = variant.repetitions, duration = variant.duration))

  def resolveAll(ops: List[Op]): Forest[Step.ID, Step] =
    resolveAll(Forest.empty, ops)

  def resolveAll(forest: Forest[Step.ID, Step], ops: List[Op]): Forest[Step.ID, Step] =
    ops.foldLeft(forest)((forest, op) => ForestResolver.resolve(forest, toUpdates(forest, op)))

  def toUpdates(forest: Forest[Step.ID, Step], op: Op): List[Update[Step.ID, Step]] = {
    val interpreter = ForestInterpreter(forest)
    op match {
      case Op.Add(id, parent, variant) => interpreter.addOption(step(id, variant), parent)
      case Op.Move(id, parent) => interpreter.move(id, parent)
      case Op.Remove(id) => interpreter.remove(id)
      case Op.Edit(id, variant) => interpreter.update(id, _ => step(id, variant))
      case op: Op.Reorder => interpreter.reorder(reordering(forest, op))
    }
  }

  /** A shuffle of the anchor's siblings, possibly corrupted so that it is not a permutation */
  private def reordering(forest: Forest[Step.ID, Step], op: Op.Reorder): List[Step.ID] = {
    val shuffled = new Random(op.seed).shuffle(forest.siblings(op.anchor))
    op.corruption match {
      case Corruption.Valid => shuffled
      case Corruption.Missing => shuffled.drop(1)
      case Corruption.Duplicated => shuffled.drop(1).headOption.fold(shuffled)(shuffled.updated(0, _))
    }
  }

  private final case class Shape(
    nodes: Map[Step.ID, Step],
    toChildren: Map[Step.ID, List[Step.ID]],
    roots: List[Step.ID]
  ) {
    def detach(id: Step.ID, parent: Option[Step.ID]): Shape =
      parent match {
        case Some(p) => copy(toChildren = toChildren.updated(p, toChildren(p).filterNot(_ == id)))
        case None => copy(roots = roots.filterNot(_ == id))
      }

    def append(id: Step.ID, parent: Option[Step.ID]): Shape =
      parent match {
        case Some(p) => copy(toChildren = toChildren.updated(p, toChildren(p) :+ id))
        case None => copy(roots = roots :+ id)
      }

    def toForest: Forest[Step.ID, Step] =
      Forest.from(nodes, toChildren, roots)
  }

  /** The forest we expect after applying the operation, derived without the interpreter */
  def expected(forest: Forest[Step.ID, Step], op: Op): Forest[Step.ID, Step] = {
    val shape = Shape(forest.nodes, forest.toChildren, forest.roots)
    op match {
      case Op.Add(id, target, variant) =>
        place(forest, shape, step(id, variant), target).toForest

      case Op.Move(id, target) =>
        forest.get(id).fold(forest)(place(forest, shape, _, target).toForest)

      case Op.Remove(id) =>
        val removed = forest.subtree(id).nodes.keySet
        Forest.from(
          forest.nodes -- removed,
          (forest.toChildren -- removed).view.mapValues(_.filterNot(removed.contains)).toMap,
          forest.roots.filterNot(removed.contains)
        )

      case Op.Edit(id, variant) =>
        if (forest.contains(id))
          shape.copy(nodes = shape.nodes.updated(id, step(id, variant))).toForest
        else
          forest

      case op: Op.Reorder =>
        if (op.corruption != Corruption.Valid || !forest.contains(op.anchor))
          forest
        else {
          val newOrder = reordering(forest, op)
          forest.toParent.get(op.anchor) match {
            case Some(parent) => shape.copy(toChildren = shape.toChildren.updated(parent, newOrder)).toForest
            case None => shape.copy(roots = newOrder).toForest
          }
        }
    }
  }

  /** Adding a step that already exists moves it. A missing target parent means the step
    * becomes a root. Steps can't be moved inside themselves, and a step that changes parent
    * becomes that parent's last child. */
  private def place(forest: Forest[Step.ID, Step], shape: Shape, data: Step, target: Option[Step.ID]): Shape = {
    val id = data.id
    val newParent = target.filter(forest.contains)
    val withData = shape.copy(nodes = shape.nodes.updated(id, data))

    if (!forest.contains(id))
      withData.copy(toChildren = withData.toChildren + (id -> List.empty)).append(id, newParent)
    else if (target.contains(id) || target.exists(forest.ancestors(_).contains(id)))
      withData
    else {
      val oldParent = forest.toParent.get(id)
      if (newParent == oldParent) withData
      else withData.detach(id, oldParent).append(id, newParent)
    }
  }

  /** Applies the batches of updates to a mock plan directory, then reads the plan back */
  def persist(batches: Seq[List[Update[Step.ID, Step]]]): Forest[Step.ID, Step] = {
    val directory = PlanDirectory(new MockDirectoryHandle)
    var result = Option.empty[Either[FileSystemError, Plan]]

    Using(new ManualOwner)(owner =>
      given ManualOwner = owner
      directory
        .create(PlanMetadata("test"), Plan("test", Forest.empty, Armageddon.settings))
        .foreach(_ => ())
      EventStream
        .fromSeq(batches.map(StepUpdates(_)))
        .flatMapConcat(directory.applyUpdate)
        .foreach(_ => ())
      directory.readPlan().foreach(plan => result = Some(plan))
    )(using _.killSubscriptions()).get

    result.getOrElse(throw new AssertionError("The persisted plan was not read")).map(_.steps) match {
      case Right(forest) => forest
      case Left(error) => throw new AssertionError(s"Failed to read the persisted plan: $error")
    }
  }
}
