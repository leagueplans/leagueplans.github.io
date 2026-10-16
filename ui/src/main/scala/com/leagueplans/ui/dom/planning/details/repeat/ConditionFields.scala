package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.common.model.Item
import com.leagueplans.ui.dom.planning.details.DurationText
import com.leagueplans.ui.model.plan.StopCondition.{ItemCount, ItemForms, ItemLocation}
import com.leagueplans.ui.model.plan.{ExpTarget, Repetitions, StopCondition}
import com.leagueplans.ui.model.player.item.Depository
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import com.leagueplans.ui.projection.calculation.loops.StopConditions.Leaf
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** The number each kind of condition is typed with: a level or an amount of xp, an amount of an
  * item, a number of free slots, or a time. Text that can't be read says why, in the cards' wording.
  */
object ConditionFields {
  /** Copies of conditions are typed as any condition, though they're of the same kind */
  def asLeaf(condition: StopCondition): Leaf =
    condition match {
      case leaf: Leaf => leaf
      case other => throw IllegalArgumentException(s"Not a single condition: $other")
    }

  def text(leaf: Leaf): String =
    leaf match {
      case StopCondition.SkillReaches(_, ExpTarget.AtLevel(level), _) => level.toString
      case StopCondition.SkillReaches(_, ExpTarget.AtExp(exp), _) => (exp.raw / 10).toString
      case leaf: StopCondition.ItemCount => leaf.quantity.toString
      case leaf: StopCondition.FreeInventorySlots => leaf.slots.toString
      case leaf: StopCondition.TimeElapsed => DurationText.format(leaf.atLeast)
    }

  /** The condition with the typed number in place of its own */
  def parse(leaf: Leaf, text: String): Either[String, Leaf] = {
    val cleaned = text.trim.replace(",", "")
    leaf match {
      case leaf @ StopCondition.SkillReaches(_, _: ExpTarget.AtLevel, _) =>
        cleaned.toIntOption.filter(n => n >= 1 && n <= 99)
          .map(n => asLeaf(leaf.copy(target = ExpTarget.AtLevel(Level(n)))))
          .toRight("Type a level from 1 to 99")
      case leaf @ StopCondition.SkillReaches(_, _: ExpTarget.AtExp, _) =>
        cleaned.toDoubleOption.map(xp => math.round(xp * 10)).filter(tenths => tenths > 0 && tenths <= Exp.max.raw)
          .map(tenths => asLeaf(leaf.copy(target = ExpTarget.AtExp(Exp.tenths(tenths.toInt)))))
          .toRight(s"Type an amount of xp up to ${(Exp.max.raw / 10).withCommas}")
      case leaf: StopCondition.ItemCount =>
        cleaned.toIntOption.filter(_ >= 0).map(n => asLeaf(leaf.copy(quantity = n))).toRight("Type an amount from 0")
      case leaf: StopCondition.FreeInventorySlots =>
        cleaned.toIntOption.filter(n => n >= 0 && n <= Depository.Kind.Inventory.capacity)
          .map(n => asLeaf(leaf.copy(slots = n)))
          .toRight(s"Type a number of slots from 0 to ${Depository.Kind.Inventory.capacity}")
      case leaf: StopCondition.TimeElapsed =>
        DurationText.parseUncapped(text) match {
          case Right(duration) if duration.length > 0 => Right(asLeaf(leaf.copy(atLeast = duration)))
          case Right(_) => Left("Type a time such as 1h 30m or 90m")
          case Left(error) => Left(error)
        }
    }
  }

  /** A step's own limit on its repetitions. Blank is none. */
  def parseMax(text: String): Either[String, Option[Int]] =
    text.trim.replace(",", "") match {
      case "" => Right(None)
      case typed =>
        typed.toIntOption.filter(n => n >= 1 && n <= Repetitions.cap).map(Some(_))
          .toRight(s"Type a number of times from 1 to ${Repetitions.cap.withCommas}")
    }

  /** Where an item can be counted: worn only if it can be equipped */
  def locations(item: Item): List[ItemLocation] =
    if (item.equipmentType.nonEmpty) ItemLocation.values.toList
    else ItemLocation.values.toList.filterNot(_ == ItemLocation.Worn)

  /** Whether a count of an item in a place says if it's of noted items: only the inventory holds
    * them, and only of items that can be noted */
  def hasForms(item: Item, where: ItemLocation): Boolean =
    item.noteable && where == ItemLocation.Inventory

  /** An item count kept to the choices its item and place allow, once either changes: an item that
    * can't be equipped is counted in the inventory instead of worn, and only an item that can be
    * noted, in the inventory, is counted as noted. Other conditions are left as they are. */
  def fitted(leaf: Leaf, items: Item.ID => Item): Leaf =
    leaf match {
      case count: ItemCount =>
        val item = items(count.item)
        val where = if (locations(item).contains(count.where)) count.where else ItemLocation.Inventory
        asLeaf(count.copy(where = where, forms = if (hasForms(item, where)) count.forms else ItemForms.Unnoted))
      case other => other
    }
}
