package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.CollectionDecoder
import com.leagueplans.codec.encoding.CollectionEncoder

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

/** A step's effects, in order. `merge.StepEffects` keeps them merged as they're changed. */
final case class EffectList(underlying: List[Effect]) extends AnyVal
