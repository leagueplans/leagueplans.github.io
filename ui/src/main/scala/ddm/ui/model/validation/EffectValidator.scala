package ddm.ui.model.validation

import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Effect.*
import ddm.ui.model.plan.{Effect, EffectList}
import ddm.ui.model.player.{Cache, Player}

sealed trait EffectValidator[E <: Effect] {
  def validate(effect: E)(player: Player, cache: Cache): (List[String], Player)
}

object EffectValidator extends EffectValidator[Effect] {
  def validate(effectList: EffectList)(player: Player, cache: Cache): (List[String], Player) =
    effectList.underlying.foldLeft((List.empty[String], player)) {
      case ((errorAcc, preEffectPlayer), effect) =>
        val (errors, postEffectPlayer) = EffectValidator.validate(effect)(preEffectPlayer, cache)
        (errorAcc ++ errors, postEffectPlayer)
    }

  def validate(effect: Effect)(player: Player, cache: Cache): (List[String], Player) =
    effect match {
      case e: GainExp => gainExpValidator.validate(e)(player, cache)
      case e: AddItem => addItemValidator.validate(e)(player, cache)
      case e: MoveItem => moveItemValidator.validate(e)(player, cache)
      case e: UnlockSkill => unlockSkillValidator.validate(e)(player, cache)
      case e: CompleteQuest => completeQuestValidator.validate(e)(player, cache)
      case e: CompleteDiaryTask => completeDiaryTaskValidator.validate(e)(player, cache)
      case e: CompleteLeagueTask => completeLeagueTaskValidator.validate(e)(player, cache)
    }

  private val gainExpValidator: EffectValidator[GainExp] =
    from(
      pre = gain => List(Validator.skillUnlocked(gain.skill)),
      post = _ => List.empty
    )

  private val addItemValidator: EffectValidator[AddItem] =
    from(
      pre = gain => if (gain.count < 0) List(Validator.hasItem(gain.target, gain.item, gain.note, -gain.count)) else List.empty,
      post = gain => if (gain.count > 0) List(Validator.depositorySize(gain.target)) else List.empty
    )

  private val moveItemValidator: EffectValidator[MoveItem] =
    from(
      pre = move => List(Validator.hasItem(move.source, move.item, move.notedInSource, move.count)),
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

  private def from[E <: Effect](
    pre: E => List[Validator],
    post: E => List[Validator]
  ): EffectValidator[E] =
    new EffectValidator[E] {
      def validate(effect: E)(player: Player, cache: Cache): (List[String], Player) = {
        val postEffectPlayer = EffectResolver.resolve(player, cache, effect)
        val errors =
          collectErrors(pre(effect))(player, cache) ++
            collectErrors(post(effect))(postEffectPlayer, cache)

        (errors, postEffectPlayer)
      }

      private def collectErrors(validators: List[Validator])(player: Player, cache: Cache): List[String] =
        validators
          .map(_.apply(player, cache))
          .collect { case Left(error) => error }
    }
}
