package com.leagueplans.ui.projection.calculation.loops

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.{Duration, ExpTarget, StopCondition}
import com.leagueplans.ui.model.plan.StopCondition.*
import com.leagueplans.ui.model.player.item.{Depository, ItemEffects}
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** Checks stop conditions against a player.
  *
  * Each leaf condition is measured by how far it still has to go, in its own units: tenths of
  * exp, items, or inventory slots. Nothing left to go means the condition is met. Less than
  * nothing means the player has gone past it.
  */
object StopConditions {
  type Leaf = SkillReaches | ItemCount | FreeInventorySlots | TimeElapsed

  def leaves(condition: StopCondition): List[Leaf] =
    condition match {
      case And(left, right) => leaves(left) ++ leaves(right)
      case Or(left, right) => leaves(left) ++ leaves(right)
      case leaf: Leaf => List(leaf)
    }

  def remaining(leaf: Leaf, context: LoopContext): Long =
    leaf match {
      case SkillReaches(skill, target, _) =>
        target.goal.raw.toLong - context.player.stats(skill).raw
      case ItemCount(item, where, forms, comparison, quantity, _) =>
        compare(comparison, count(item, where, forms, context), quantity)
      case FreeInventorySlots(comparison, slots, _) =>
        compare(comparison, ItemEffects.freeInventorySlots(context.player, context.items).toLong, slots)
      // In milliseconds
      case TimeElapsed(since, atLeast, _) =>
        val elapsed = since match {
          case TimeOrigin.StepStart => context.inStep
          case TimeOrigin.PlanStart => context.inPlan
        }
        if (elapsed.isFinite) atLeast.asScala.toMillis - elapsed.toMillis else -1
    }

  private def compare(comparison: Comparison, actual: Long, threshold: Int): Long =
    comparison match {
      case Comparison.AtLeast => threshold - actual
      case Comparison.AtMost => actual - threshold
    }

  /** What's held, less anything the repetition tried to take but couldn't */
  private def count(item: Item.ID, where: ItemLocation, forms: ItemForms, context: LoopContext): Long =
    (for {
      place <- places(where)
      noted <- notedness(forms)
    } yield context.player.get(place).count(item, noted).toLong - context.shortfalls.getOrElse((item, place, noted), 0)).sum

  private def places(where: ItemLocation): List[Depository.Kind] =
    where match {
      case ItemLocation.Inventory => List(Depository.Kind.Inventory)
      case ItemLocation.Bank => List(Depository.Kind.Bank)
      case ItemLocation.Worn => Depository.Kind.EquipmentSlot.values.toList
    }

  private def notedness(forms: ItemForms): List[Boolean] =
    forms match {
      case ItemForms.Unnoted => List(false)
      case ItemForms.Noted => List(true)
    }

  def met(condition: StopCondition, context: LoopContext): Boolean =
    condition match {
      case And(left, right) => met(left, context) && met(right, context)
      case Or(left, right) => met(left, context) || met(right, context)
      case leaf: Leaf => remaining(leaf, context) <= 0
    }

  /** Whether the condition is met before another repetition, counting a condition that asks to stop
    * before it's passed as met once the next repetition would pass it.
    */
  def metBefore(condition: StopCondition, now: LoopContext, next: LoopContext): Boolean =
    condition match {
      case And(left, right) => metBefore(left, now, next) && metBefore(right, now, next)
      case Or(left, right) => metBefore(left, now, next) || metBefore(right, now, next)
      case leaf: Leaf => remaining(leaf, now) <= 0 || (stopsBefore(leaf) && remaining(leaf, next) < 0)
    }

  /** The conditions asking to stop before they're passed that a repetition took past their
    * thresholds */
  def passed(condition: StopCondition, before: LoopContext, after: LoopContext): List[Leaf] =
    leaves(condition).filter(leaf => stopsBefore(leaf) && remaining(leaf, before) >= 0 && remaining(leaf, after) < 0)

  def stopsBefore(leaf: Leaf): Boolean =
    leaf match {
      case l: SkillReaches => l.stopBefore
      case l: ItemCount => l.stopBefore
      case l: FreeInventorySlots => l.stopBefore
      case l: TimeElapsed => l.stopBefore
    }

  /** Whether a repetition brought the condition closer to being met. Conditions combined with "and"
    * all have to get closer, apart from those already met. Of conditions combined with "or", any one
    * getting closer will do.
    */
  def progressed(condition: StopCondition, before: LoopContext, after: LoopContext): Boolean =
    condition match {
      case And(left, right) =>
        List(left, right).filterNot(met(_, after)).forall(progressed(_, before, after))
      case Or(left, right) =>
        progressed(left, before, after) || progressed(right, before, after)
      case leaf: Leaf =>
        remaining(leaf, after) < remaining(leaf, before)
    }

  /** About how many more repetitions it would take to meet the condition, if each went as the last
    * one did, or None if the last one brought it no closer. This is only ever a guess, since a
    * repetition can make more or less progress than the one before.
    */
  def estimate(condition: StopCondition, before: LoopContext, after: LoopContext): Option[Long] =
    condition match {
      case And(left, right) =>
        List(left, right).filterNot(met(_, after)).map(estimate(_, before, after)) match {
          case estimates if estimates.forall(_.nonEmpty) => Some(estimates.flatten.maxOption.getOrElse(0))
          case _ => None
        }
      case Or(left, right) =>
        List(estimate(left, before, after), estimate(right, before, after)).flatten.minOption
      case leaf: Leaf =>
        val left = remaining(leaf, after)
        val progress = remaining(leaf, before) - left
        if (left <= 0) Some(0)
        else Option.when(progress > 0)((left + progress - 1) / progress)
    }

  /** Such as "Hunter reaches level 60 or the inventory is full" */
  def describe(condition: StopCondition, items: Item.ID => Item): String =
    condition match {
      case And(left, right) => s"${describeWithin(left, "and", items)} and ${describeWithin(right, "and", items)}"
      case Or(left, right) => s"${describeWithin(left, "or", items)} or ${describeWithin(right, "or", items)}"
      case leaf: Leaf => describeLeaf(leaf, items)
    }

  private def describeWithin(condition: StopCondition, joiner: String, items: Item.ID => Item): String =
    condition match {
      case _: And if joiner != "and" => s"(${describe(condition, items)})"
      case _: Or if joiner != "or" => s"(${describe(condition, items)})"
      case _ => describe(condition, items)
    }

  def describeLeaf(leaf: Leaf, items: Item.ID => Item): String =
    leaf match {
      case SkillReaches(skill, ExpTarget.AtLevel(level), _) =>
        s"$skill reaches level $level"
      case SkillReaches(skill, ExpTarget.AtExp(exp), _) =>
        s"$skill reaches ${(exp.raw / 10).withCommas} xp"
      case ItemCount(item, where, forms, comparison, quantity, _) =>
        val amount = s"${describe(comparison)} ${quantity.withCommas} ${name(item, forms, items)}"
        where match {
          case ItemLocation.Inventory => s"the inventory holds $amount"
          case ItemLocation.Bank => s"the bank holds $amount"
          case ItemLocation.Worn => s"$amount ${if (quantity == 1) "is" else "are"} worn"
        }
      case FreeInventorySlots(Comparison.AtMost, 0, _) =>
        "the inventory is full"
      case FreeInventorySlots(comparison, slots, _) =>
        s"the inventory has ${describe(comparison)} ${slots.withCommas} free ${if (slots == 1) "slot" else "slots"}"
      case TimeElapsed(TimeOrigin.StepStart, atLeast, _) =>
        s"this step has taken ${describe(atLeast)}"
      case TimeElapsed(TimeOrigin.PlanStart, atLeast, _) =>
        s"the plan reaches ${describe(atLeast)}"
    }

  /** Such as "Feather in the inventory below 0", for a repetition that takes a condition past its
    * threshold */
  def describePassing(leaf: Leaf, items: Item.ID => Item): String =
    leaf match {
      case SkillReaches(skill, ExpTarget.AtLevel(level), _) =>
        s"$skill past level $level"
      case SkillReaches(skill, ExpTarget.AtExp(exp), _) =>
        s"$skill past ${(exp.raw / 10).withCommas} xp"
      case ItemCount(item, where, forms, comparison, quantity, _) =>
        s"${name(item, forms, items)} ${describe(where)} ${direction(comparison)} ${quantity.withCommas}"
      case FreeInventorySlots(comparison, slots, _) =>
        s"the inventory's free slots ${direction(comparison)} ${slots.withCommas}"
      case TimeElapsed(TimeOrigin.StepStart, atLeast, _) =>
        s"the time in this step past ${describe(atLeast)}"
      case TimeElapsed(TimeOrigin.PlanStart, atLeast, _) =>
        s"the plan past ${describe(atLeast)}"
    }

  /** Such as "1h 30m" or "45 ticks" */
  private def describe(duration: Duration): String =
    duration.unit match {
      case Duration.Unit.Ticks =>
        s"${duration.length.withCommas} ${if (duration.length == 1) "tick" else "ticks"}"
      case Duration.Unit.Seconds =>
        val (h, m, s) = (duration.length / 3600, duration.length / 60 % 60, duration.length % 60)
        List(Option.when(h > 0)(s"${h.withCommas}h"), Option.when(m > 0)(s"${m}m"), Option.when(s > 0 || duration.length == 0)(s"${s}s"))
          .flatten.mkString(" ")
    }

  private def name(item: Item.ID, forms: ItemForms, items: Item.ID => Item): String =
    forms match {
      case ItemForms.Unnoted => items(item).fullName
      case ItemForms.Noted => s"${items(item).fullName} (noted)"
    }

  private def direction(comparison: Comparison): String =
    comparison match {
      case Comparison.AtLeast => "above"
      case Comparison.AtMost => "below"
    }

  private def describe(comparison: Comparison): String =
    comparison match {
      case Comparison.AtLeast => "at least"
      case Comparison.AtMost => "at most"
    }

  private def describe(where: ItemLocation): String =
    where match {
      case ItemLocation.Inventory => "in the inventory"
      case ItemLocation.Bank => "in the bank"
      case ItemLocation.Worn => "worn"
    }
}
