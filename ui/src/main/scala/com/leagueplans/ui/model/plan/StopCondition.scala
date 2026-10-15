package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{Item, Skill}

// Saved plans refer to these cases by their position, so new cases go at the end

/** What a step that repeats until a condition is met waits for.
  *
  * With `stopBefore`, a repetition that would take the player past the condition's threshold
  * isn't played, so "until feathers ≤ 0" never runs out of feathers partway through.
  */
enum StopCondition {
  case SkillReaches(skill: Skill, target: ExpTarget, stopBefore: Boolean)

  case ItemCount(
    item: Item.ID,
    where: StopCondition.ItemLocation,
    forms: StopCondition.ItemForms,
    comparison: StopCondition.Comparison,
    quantity: Int,
    stopBefore: Boolean
  )

  /** A comparison against the number of empty inventory slots. "The inventory is full" is at
    * most 0 free slots. */
  case FreeInventorySlots(comparison: StopCondition.Comparison, slots: Int, stopBefore: Boolean)

  case And(left: StopCondition, right: StopCondition)
  case Or(left: StopCondition, right: StopCondition)

  /** At least this much time has passed, since the step started or since the plan did. The second
    * is for events held at a set time. */
  case TimeElapsed(since: StopCondition.TimeOrigin, atLeast: Duration, stopBefore: Boolean)
}

object StopCondition {
  given Encoder[StopCondition] = Encoder.derived
  given Decoder[StopCondition] = Decoder.derived

  /** Where the items an item count counts are held */
  enum ItemLocation {
    case Inventory, Bank, Worn
  }

  object ItemLocation {
    given Encoder[ItemLocation] = Encoder.derived
    given Decoder[ItemLocation] = Decoder.derived
  }

  /** Whether an item count counts the item or its noted form */
  enum ItemForms {
    case Unnoted, Noted
  }

  object ItemForms {
    given Encoder[ItemForms] = Encoder.derived
    given Decoder[ItemForms] = Decoder.derived
  }

  enum TimeOrigin {
    case StepStart, PlanStart
  }

  object TimeOrigin {
    given Encoder[TimeOrigin] = Encoder.derived
    given Decoder[TimeOrigin] = Decoder.derived
  }

  enum Comparison {
    case AtLeast, AtMost
  }

  object Comparison {
    given Encoder[Comparison] = Encoder.derived
    given Decoder[Comparison] = Decoder.derived
  }
}
