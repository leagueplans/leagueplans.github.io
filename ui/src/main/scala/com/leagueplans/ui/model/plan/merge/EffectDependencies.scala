package com.leagueplans.ui.model.plan.merge

import com.leagueplans.common.model.{Item, Skill}
import com.leagueplans.ui.model.plan.{Effect, ItemChange, ItemQuantity}
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.player.item.{BankSpace, Depository, EquipPlan}
import com.leagueplans.ui.model.player.item.Depository.Kind

/** What an effect depends on and changes, so that a step can tell when two of its effects can
  * swap places without changing what the step does. An effect can only merge with an earlier
  * one if it can move back next to it.
  */
private[merge] object EffectDependencies {
  enum Resource {
    /** The items in a stack. `None` stands for any item, or either noted state. */
    case Stack(item: Option[Item.ID], noted: Option[Boolean], place: Depository.Kind)
    /** The free space in the inventory or an equipment slot. Space in the bank isn't tracked:
      * it's rarely short, and tracking it would stop most deposits and withdrawals merging. */
    case Space(place: Depository.Kind)
    /** Any unnoted items that can be worn in the slots, in a place. What equipping takes off is
      * one of these, put back where the equipped item came from. */
    case Wearable(slots: Set[Kind.EquipmentSlot], place: Depository.Kind)
    /** What multiplies the exp an effect gives. League points and grid tiles can raise it, so exp
      * can't move past what earns them. Levels can raise it too in some modes, but exp effects
      * still move past each other, so that exp for one skill keeps merging around another's. */
    case ExpMultiplier
    /** A skill's exp, which an effect aiming for a target works out from */
    case SkillExp(skill: Skill)
  }

  /** @param reads what the effect's result depends on, for an effect that's worked out where it
    *              applies, such as a move of everything held
    * @param supplies what the effect adds to
    * @param consumes what the effect takes from
    */
  final case class Access(reads: Set[Resource], supplies: Set[Resource], consumes: Set[Resource]) {
    def writes: Set[Resource] = supplies ++ consumes
  }

  private val none = Access(Set.empty, Set.empty, Set.empty)

  /** Whether the later effect can move before the earlier one. Neither can depend on what the
    * other changes, and the later one can't take what the earlier one supplied, which it may
    * have needed. Taking from a stack or a place's space before it's added to could leave too
    * little, or overfill the place.
    */
  def canMoveBefore(later: Effect, earlier: Effect, items: Item.ID => Item): Boolean = {
    val (l, e) = (access(later, items), access(earlier, items))
    val overlaps = overlapping(items)
    !overlaps(e.writes, l.reads) && !overlaps(l.writes, e.reads) && !overlaps(e.supplies, l.consumes)
  }

  def access(effect: Effect, items: Item.ID => Item): Access =
    effect match {
      case AddItem(item, change, place, noted) =>
        val stack = Resource.Stack(Some(item), Some(noted), place)
        change match {
          case ItemChange.By(n) if n < 0 => Access(Set.empty, space(place), Set(stack))
          case ItemChange.By(_) => Access(Set.empty, Set(stack), space(place))
          case ItemChange.Fill => Access(Set(stack) ++ space(place), Set(stack), space(place))
          case ItemChange.Empty => Access(Set(stack), space(place), Set(stack))
        }

      case move @ MoveItem(item, quantity, source, notedInSource, target, noteInTarget) =>
        val from = Resource.Stack(Some(item), Some(notedInSource), source)
        val to = Resource.Stack(Some(item), Some(noteInTarget), target)
        val reads = quantity match {
          case ItemQuantity.Exact(_) => Set.empty
          // Everything held, and a withdrawal stops at what fits
          case ItemQuantity.Max => Set(from) ++ Option.when(target == Kind.Inventory)(to) ++ space(target)
        }
        val moved = Access(reads, Set(to) ++ space(source), Set(from) ++ space(target))
        target match {
          // Equipping takes off whatever's in the way, which depends on what's worn there, and
          // puts it where the item came from
          case slot: Kind.EquipmentSlot if EquipPlan.equips(move, items) =>
            val slots = EquipPlan.slotsCleared(slot)
            val worn = slots.map(Resource.Stack(None, None, _))
            val returnTo = EquipPlan.returnTo(source)
            Access(
              moved.reads ++ worn,
              moved.supplies ++ slots.flatMap(space) + Resource.Wearable(slots, returnTo),
              moved.consumes ++ worn ++ space(returnTo)
            )
          case _ =>
            moved
        }

      case DepositAll(source) =>
        val held = source.places.map(Resource.Stack(None, None, _)).toSet
        Access(held, source.places.flatMap(space).toSet + Resource.Stack(None, Some(false), Kind.Bank), held)

      // The coins come from the inventory or the bank, depending on which holds enough, and
      // taking the inventory's last coins frees a slot. The bank's space isn't tracked.
      case BuyBankSpace(_) =>
        val coins = Set(Kind.Inventory, Kind.Bank).map(place => Resource.Stack(Some(BankSpace.coins), Some(false), place))
        Access(coins, space(Kind.Inventory), coins)

      case GainExp(skill, _, _) =>
        Access(Set(Resource.ExpMultiplier), Set(Resource.SkillExp(skill)), Set.empty)

      case GainExpToTarget(skill, _, _) =>
        Access(Set(Resource.ExpMultiplier, Resource.SkillExp(skill)), Set(Resource.SkillExp(skill)), Set.empty)

      case _: (CompleteLeagueTask | CompleteGridTile) =>
        Access(Set.empty, Set(Resource.ExpMultiplier), Set.empty)

      case _: (UnlockSkill | CompleteQuest | CompleteDiaryTask) | SetBankPin =>
        none
    }

  private def space(place: Depository.Kind): Set[Resource] =
    if (place == Kind.Bank) Set.empty else Set(Resource.Space(place))

  private def overlapping(items: Item.ID => Item)(a: Set[Resource], b: Set[Resource]): Boolean =
    a.exists(x => b.exists(overlap(x, _, items)))

  private def overlap(a: Resource, b: Resource, items: Item.ID => Item): Boolean =
    (a, b) match {
      case (Resource.Stack(itemA, notedA, placeA), Resource.Stack(itemB, notedB, placeB)) =>
        placeA == placeB && matches(itemA, itemB) && matches(notedA, notedB)
      case (Resource.Space(placeA), Resource.Space(placeB)) =>
        placeA == placeB
      case (Resource.Wearable(slots, placeA), Resource.Stack(item, noted, placeB)) =>
        placeA == placeB && matches(noted, Some(false)) && item.forall(wornIn(_, slots, items))
      case (stack: Resource.Stack, wearable: Resource.Wearable) =>
        overlap(wearable, stack, items)
      case (Resource.Wearable(slotsA, placeA), Resource.Wearable(slotsB, placeB)) =>
        placeA == placeB && slotsA.intersect(slotsB).nonEmpty
      case (Resource.ExpMultiplier, Resource.ExpMultiplier) =>
        true
      case (Resource.SkillExp(skillA), Resource.SkillExp(skillB)) =>
        skillA == skillB
      case _ =>
        false
    }

  private def wornIn(item: Item.ID, slots: Set[Kind.EquipmentSlot], items: Item.ID => Item): Boolean =
    items(item).equipmentType.map(Kind.EquipmentSlot.from).exists(slots.contains)

  private def matches[T](a: Option[T], b: Option[T]): Boolean =
    a.isEmpty || b.isEmpty || a == b
}
