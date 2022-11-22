package ddm.ui.model.plan

import ddm.ui.model.plan.Effect._

import scala.reflect.ClassTag

object EffectList {
  val empty: EffectList = EffectList(List.empty)
}

final case class EffectList(underlying: List[Effect]) {
  def +(effect: Effect): EffectList =
    effect match {
      case e: GainExp => add(e)
      case e: GainItem => add(e)
      case e: MoveItem => add(e)
      case _: UnlockSkill | _: SetMultiplier | _: CompleteQuest | _: CompleteTask =>
        ignoreDuplicates(effect)
    }

  def -(effect: Effect): EffectList =
    EffectList(underlying.filterNot(_ == effect))

  private def add(effect: GainExp): EffectList =
    patch(effect)(_.skill == _.skill)((oldEffect, newEffect) =>
      Some(oldEffect.copy(baseExp = oldEffect.baseExp + newEffect.baseExp))
        .filter(_.baseExp.raw != 0)
    )

  private def add(effect: GainItem): EffectList =
    patch(effect)((oldEffect, newEffect) =>
      oldEffect.item == newEffect.item && oldEffect.target == newEffect.target
    )((oldEffect, newEffect) =>
      Some(oldEffect.copy(count = oldEffect.count + newEffect.count))
        .filter(_.count != 0)
    )

  private def add(effect: MoveItem): EffectList =
    patch(effect)((oldEffect, newEffect) =>
      oldEffect.item == newEffect.item && (
        (oldEffect.source == newEffect.source && oldEffect.target == newEffect.target) ||
          (oldEffect.source == newEffect.target && oldEffect.target == newEffect.source)
      )
    )((oldEffect, newEffect) =>
      if (oldEffect.target == newEffect.target)
        Some(oldEffect.copy(count = oldEffect.count + newEffect.count))
      else if (oldEffect.count > newEffect.count)
        Some(oldEffect.copy(count = oldEffect.count - newEffect.count))
      else if (oldEffect.count < newEffect.count)
        Some(newEffect.copy(count = newEffect.count - oldEffect.count))
      else
        None
    )

  private def ignoreDuplicates(effect: Effect): EffectList =
    patch(effect)(_ == _)((oldEffect, _) => Some(oldEffect))

  private def patch[E <: Effect : ClassTag](target: E)(
    conflictIdentifier: (E, E) => Boolean
  )(
    conflictResolver: (E, E) => Option[E]
  ): EffectList = {
    var maybeConflict: Option[E] = None

    val conflictIndex = underlying.indexWhere {
      case e: E =>
        val conflicted = conflictIdentifier(e, target)
        if (conflicted) maybeConflict = Some(e)
        conflicted
      case _ =>
        false
    }

    maybeConflict match {
      case None =>
        EffectList(underlying :+ target)
      case Some(conflict) =>
        EffectList(underlying.patch(
          from = conflictIndex,
          conflictResolver(conflict, target),
          replaced = 1
        ))
    }
  }
}
