package com.leagueplans.ui.projection.calculation.validation

import com.leagueplans.ui.model.plan.Effect.*
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.model.player.{Cache, Player}

sealed trait EffectValidator[E <: Effect] {
  def validate(effect: E)(
    preEffectPlayer: Player,
    postEffectPlayer: Player,
    league: Option[Mode.League],
    cache: Cache
  ): List[String]
}

object EffectValidator extends EffectValidator[Effect] {
  def validate(effect: Effect)(
    preEffectPlayer: Player,
    postEffectPlayer: Player,
    league: Option[Mode.League],
    cache: Cache
  ): List[String] =
    effect match {
      case e: GainExp => gainExpValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: AddItem => addItemValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: MoveItem => moveItemValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: UnlockSkill => unlockSkillValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: CompleteQuest => completeQuestValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: CompleteDiaryTask => completeDiaryTaskValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: CompleteLeagueTask => completeLeagueTaskValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
      case e: CompleteGridTile => completeGridTileValidator.validate(e)(preEffectPlayer, postEffectPlayer, league, cache)
    }

  private val gainExpValidator: EffectValidator[GainExp] =
    from(
      pre = gain => List(Validator.skillUnlocked(gain.skill)),
      post = _ => List.empty
    )

  private val addItemValidator: EffectValidator[AddItem] =
    from(
      pre = gain => if (gain.quantity < 0) List(Validator.hasItem(gain.target, gain.item, gain.note, -gain.quantity)) else List.empty,
      post = gain => if (gain.quantity > 0) List(Validator.depositorySize(gain.target)) else List.empty
    )

  private val moveItemValidator: EffectValidator[MoveItem] =
    from(
      pre = move => List(Validator.hasItem(move.source, move.item, move.notedInSource, move.quantity)),
      post = move => List(Validator.depositorySize(move.target))
    )

  private val unlockSkillValidator: EffectValidator[UnlockSkill] =
    from(
      pre = _ => List.empty,
      post = _ => List.empty
    )

  private val completeQuestValidator: EffectValidator[CompleteQuest] =
    from(
      pre = effect => List(Validator.questIncomplete(effect.quest)),
      post = _ => List.empty
    )

  private val completeDiaryTaskValidator: EffectValidator[CompleteDiaryTask] =
    from(
      pre = effect => List(Validator.diaryTaskIncomplete(effect.task)),
      post = _ => List.empty
    )

  private val completeLeagueTaskValidator: EffectValidator[CompleteLeagueTask] =
    from(
      pre = effect => List(Validator.leagueTaskIncomplete(effect.task), Validator.leagueTaskIsPartOfLeague(effect.task)),
      post = _ => List.empty
    )

  private val completeGridTileValidator: EffectValidator[CompleteGridTile] =
    from(
      pre = effect => List(Validator.gridTileIncomplete(effect.tile)),
      post = _ => List.empty
    )

  private def from[E <: Effect](
    pre: E => List[Validator],
    post: E => List[Validator]
  ): EffectValidator[E] =
    new EffectValidator[E] {
      def validate(effect: E)(
        preEffectPlayer: Player,
        postEffectPlayer: Player,
        league: Option[Mode.League],
        cache: Cache
      ): List[String] =
        collectErrors(pre(effect))(preEffectPlayer, league, cache) ++
          collectErrors(post(effect))(postEffectPlayer, league, cache)

      private def collectErrors(validators: List[Validator])(
        player: Player,
        league: Option[Mode.League],
        cache: Cache
      ): List[String] =
        validators
          .map(_.apply(player, league, cache))
          .collect { case Left(error) => error }
    }
}
