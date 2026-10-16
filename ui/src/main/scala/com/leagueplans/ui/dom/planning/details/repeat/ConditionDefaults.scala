package com.leagueplans.ui.dom.planning.details.repeat

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.StopCondition.{Comparison, ItemForms, ItemLocation, TimeOrigin}
import com.leagueplans.ui.model.plan.{Duration, Effect, ExpTarget, Step, StopCondition}
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.item.{Depository, ItemEffects}
import com.leagueplans.ui.model.player.skill.Level
import com.leagueplans.ui.projection.calculation.loops.StopConditions.Leaf

/** What a new condition starts as, filled in from the step's own effects where they say anything
  * about it. A condition that needs no more typing is complete, and is added straight away. The rest
  * start as drafts, whose number is only a suggestion until one is typed.
  */
object ConditionDefaults {
  enum Use {
    case Removes, Adds
  }

  /** An item one of the step's effects adds or removes, with where and in which form */
  final case class OwnItem(item: Item.ID, use: Use, where: ItemLocation, forms: ItemForms)

  /** @param complete whether the condition is ready to add as it is */
  final case class Start(condition: Leaf, complete: Boolean)

  /** The skills the step's effects give xp in, in the order the effects name them */
  def ownSkills(step: Step): List[Skill] =
    step.directEffects.underlying.collect {
      case Effect.GainExp(skill, _, _) => skill
      case Effect.GainExpToTarget(skill, _, _) => skill
    }.distinct

  /** The items the step's effects add or remove, in the order the effects name them. An item named
    * by more than one effect is as the first has it. */
  def ownItems(step: Step): List[OwnItem] =
    step.directEffects.underlying.collect {
      case Effect.AddItem(item, change, target, note) =>
        OwnItem(item, if (change.removes) Use.Removes else Use.Adds, location(target), forms(note))
      case Effect.MoveItem(item, _, _, _, Depository.Kind.Inventory, noteInTarget) =>
        OwnItem(item, Use.Adds, ItemLocation.Inventory, forms(noteInTarget))
      case Effect.MoveItem(item, _, Depository.Kind.Inventory, notedInSource, _, _) =>
        OwnItem(item, Use.Removes, ItemLocation.Inventory, forms(notedInSource))
      case Effect.MoveItem(item, _, _, _, target, noteInTarget) =>
        OwnItem(item, Use.Adds, location(target), forms(noteInTarget))
    }.distinctBy(_.item)

  /** Reaching the level after the player's, typed to confirm */
  def skill(skill: Skill, player: Player): Start = {
    val next = Level.of(player.stats(skill)).next.getOrElse(Level.L99)
    Start(StopCondition.SkillReaches(skill, ExpTarget.AtLevel(next), stopBefore = false), complete = false)
  }

  /** An item the step removes repeats until it runs out, stopping before a repetition that would
    * want more than is left. Any other item repeats until there are enough, suggesting as many as
    * fill the inventory. */
  def item(item: Item, own: Option[OwnItem], player: Player, items: Item.ID => Item): Start =
    own match {
      case Some(OwnItem(_, Use.Removes, where, forms)) =>
        Start(StopCondition.ItemCount(item.id, where, forms, Comparison.AtMost, 0, stopBefore = true), complete = true)
      case _ =>
        val where = own.fold(ItemLocation.Inventory)(_.where)
        val forms = own.fold(ItemForms.Unnoted)(_.forms)
        val target = suggestedAmount(item, where, forms, player, items)
        Start(StopCondition.ItemCount(item.id, where, forms, Comparison.AtLeast, target, stopBefore = false), complete = false)
    }

  /** Until the inventory is full */
  val freeSlots: Start =
    Start(StopCondition.FreeInventorySlots(Comparison.AtMost, 0, stopBefore = false), complete = true)

  /** Until the step has taken a time, typed to confirm */
  val time: Start =
    Start(StopCondition.TimeElapsed(TimeOrigin.StepStart, Duration.seconds(5400), stopBefore = false), complete = false)

  /** As many as fill the inventory, for an item that takes a slot each. A stack can grow without
    * limit, so for one of those, and elsewhere than the inventory, it's the next round thousand that's at
    * least 600 more than are held. */
  private def suggestedAmount(item: Item, where: ItemLocation, forms: ItemForms, player: Player, items: Item.ID => Item): Int = {
    val noted = forms == ItemForms.Noted
    val held = places(where).map(player.get(_).count(item.id, noted)).sum
    if (where == ItemLocation.Inventory && !item.stackable && !noted)
      held + ItemEffects.freeInventorySlots(player, items)
    else
      (held + 600 + 999) / 1000 * 1000
  }

  private def places(where: ItemLocation): List[Depository.Kind] =
    where match {
      case ItemLocation.Inventory => List(Depository.Kind.Inventory)
      case ItemLocation.Bank => List(Depository.Kind.Bank)
      case ItemLocation.Worn => Depository.Kind.EquipmentSlot.values.toList
    }

  private def location(kind: Depository.Kind): ItemLocation =
    kind match {
      case Depository.Kind.Inventory => ItemLocation.Inventory
      case Depository.Kind.Bank => ItemLocation.Bank
      case _: Depository.Kind.EquipmentSlot => ItemLocation.Worn
    }

  private def forms(noted: Boolean): ItemForms =
    if (noted) ItemForms.Noted else ItemForms.Unnoted
}
