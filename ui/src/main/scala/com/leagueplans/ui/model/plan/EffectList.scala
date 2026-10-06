package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.CollectionDecoder
import com.leagueplans.codec.encoding.CollectionEncoder
import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.plan.ItemQuantity.Exact
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
      case _: UnlockSkill | _: CompleteQuest | _: CompleteDiaryTask | _: CompleteLeagueTask | _: CompleteGridTile | _: DepositAll =>
        ignoreDuplicates(effect)
    }

  def -(effect: Effect): EffectList =
    EffectList(underlying.filterNot(_ == effect))

  private def add(effect: GainExp): EffectList =
    patch(effect)(_.skill == _.skill)((oldEffect, newEffect) =>
      Some(oldEffect.copy(baseExp = oldEffect.baseExp + newEffect.baseExp))
        .filter(_.baseExp != Exp(0))
    )

  // Exact amounts add up, and cancel out at nothing. Fill and Empty are worked out where they
  // apply, so they stay their own effects.
  private def add(effect: AddItem): EffectList =
    patch(effect)((oldEffect, newEffect) =>
      oldEffect.item == newEffect.item &&
        oldEffect.target == newEffect.target &&
        oldEffect.note == newEffect.note &&
        oldEffect.change.isInstanceOf[ItemChange.By] &&
        newEffect.change.isInstanceOf[ItemChange.By]
    )((oldEffect, newEffect) =>
      (oldEffect.change, newEffect.change) match {
        case (ItemChange.By(a), ItemChange.By(b)) => Option.when(a + b != 0)(oldEffect.copy(change = ItemChange.By(a + b)))
        case _ => Some(newEffect)
      }
    )

  private def bothExact(a: ItemQuantity, b: ItemQuantity): Boolean =
    a.isInstanceOf[Exact] && b.isInstanceOf[Exact]

  private def exact(quantity: ItemQuantity): Int =
    quantity match {
      case Exact(n) => n
      case ItemQuantity.Max => throw IllegalArgumentException("Only exact amounts combine")
    }

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
      oldEffect.item == newEffect.item && bothExact(oldEffect.quantity, newEffect.quantity) && (
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
      val (oldCount, newCount) = (exact(oldEffect.quantity), exact(newEffect.quantity))
      if (oldEffect.target == newEffect.target)
        Some(oldEffect.copy(quantity = Exact(oldCount + newCount)))
      else if (oldCount > newCount)
        Some(oldEffect.copy(quantity = Exact(oldCount - newCount)))
      else if (oldCount < newCount)
        Some(newEffect.copy(quantity = Exact(newCount - oldCount)))
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
