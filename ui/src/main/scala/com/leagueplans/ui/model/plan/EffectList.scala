package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.CollectionDecoder
import com.leagueplans.codec.encoding.CollectionEncoder
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.player.skill.Exp

import scala.reflect.TypeTest

object EffectList {
  val empty: EffectList = EffectList(List.empty)
  
  given CollectionEncoder[EffectList] =
    CollectionEncoder
      .iterableOnceEncoder[List, Effect]
      .contramap(_.underlying)

  given CollectionDecoder[EffectList] =
    CollectionDecoder
      .iterableOnceDecoder[List, Effect]
      .map(EffectList.apply)
}

final case class EffectList(underlying: List[Effect]) extends AnyVal {
  def +(effect: Effect): EffectList =
    effect match {
      case e: GainExp => add(e)
      case e: AddItem => add(e)
      case e: MoveItem => add(e)
      case _: UnlockSkill | _: CompleteQuest | _: CompleteDiaryTask | _: CompleteLeagueTask =>
        ignoreDuplicates(effect)
    }

  def -(effect: Effect): EffectList =
    EffectList(underlying.filterNot(_ == effect))

  private def add(effect: GainExp): EffectList =
    patch(effect)(_.skill == _.skill)((oldEffect, newEffect) =>
      Some(oldEffect.copy(baseExp = oldEffect.baseExp + newEffect.baseExp))
        .filter(_.baseExp != Exp(0))
    )

  private def add(effect: AddItem): EffectList =
    patch(effect)((oldEffect, newEffect) =>
      oldEffect.item == newEffect.item &&
        oldEffect.target == newEffect.target &&
        oldEffect.note == newEffect.note
    )((oldEffect, newEffect) =>
      Some(oldEffect.copy(quantity = oldEffect.quantity + newEffect.quantity))
        .filter(_.quantity != 0)
    )

  // In theory you can minimise more moves than this.
  // For example, `bank -> inventory -> equipped` can be shortened to
  // `bank -> equipped`.
  // There's an interesting paper that effectively covers this topic titled:
  // Settling Multiple Debts Efficiently: An Invitation to Computing Science
  // We're effectively trying to construct a bipartite graph with a minimised
  // number of edges. The paper highlights that this is suspected to be an NP
  // hard problem.
  //
  // If I ever look back into this, the advantage I do have is that I'm only
  // ever adding a single edge to a graph that is already bipartite with a
  // minimum number of edges. I suspect that's not a good enough
  // simplification to avoid the NP-ness though (I'm thinking about the case
  // where you add an edge between two distinct subgraphs).
  private def add(effect: MoveItem): EffectList =
    patch(effect)((oldEffect, newEffect) =>
      oldEffect.item == newEffect.item && (
        (
          oldEffect.source == newEffect.source &&
            oldEffect.notedInSource == newEffect.notedInSource &&
            oldEffect.target == newEffect.target &&
            oldEffect.noteInTarget == newEffect.noteInTarget
        ) || (
          oldEffect.source == newEffect.target &&
            oldEffect.notedInSource == newEffect.noteInTarget &&
            oldEffect.target == newEffect.source &&
            oldEffect.noteInTarget == newEffect.notedInSource
        )
      )
    )((oldEffect, newEffect) =>
      if (oldEffect.target == newEffect.target)
        Some(oldEffect.copy(quantity = oldEffect.quantity + newEffect.quantity))
      else if (oldEffect.quantity > newEffect.quantity)
        Some(oldEffect.copy(quantity = oldEffect.quantity - newEffect.quantity))
      else if (oldEffect.quantity < newEffect.quantity)
        Some(newEffect.copy(quantity = newEffect.quantity - oldEffect.quantity))
      else
        None
    )

  private def ignoreDuplicates(effect: Effect): EffectList =
    patch(effect)(_ == _)((oldEffect, _) => Some(oldEffect))

  private def patch[E <: Effect](target: E)(
    conflictIdentifier: (E, E) => Boolean
  )(
    conflictResolver: (E, E) => Option[E]
  )(using TypeTest[Effect, E]): EffectList = {
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
