package ddm.ui.model.validation

import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Effect._
import ddm.ui.model.plan.{Effect, EffectList}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache

sealed trait EffectValidator[E <: Effect] {
  def validate(effect: E)(player: Player, itemCache: ItemCache): (List[String], Player)
}

object EffectValidator extends EffectValidator[Effect] {
  def validate(effectList: EffectList)(player: Player, itemCache: ItemCache): List[String] = {
    val (_, errors) = effectList.underlying.foldLeft((player, List.empty[String])) {
      case ((preEffectPlayer, errorAcc), effect) =>
        val (errors, postEffectPlayer) = EffectValidator.validate(effect)(preEffectPlayer, itemCache)
        (postEffectPlayer, errorAcc ++ errors)
    }
    errors
  }

  def validate(effect: Effect)(player: Player, itemCache: ItemCache): (List[String], Player) =
    effect match {
      case e: GainExp => gainExpValidator.validate(e)(player, itemCache)
      case e: AddItem => addItemValidator.validate(e)(player, itemCache)
      case e: MoveItem => moveItemValidator.validate(e)(player, itemCache)
      case e: UnlockSkill => unlockSkillValidator.validate(e)(player, itemCache)
      case e: CompleteQuest => completeQuestValidator.validate(e)(player, itemCache)
      case e: SetMultiplier => empty.validate(e)(player, itemCache)
      case e: CompleteTask => completeTaskValidator.validate(e)(player, itemCache)
    }

  private def empty[E <: Effect]: EffectValidator[E] =
    from(_ => List.empty, _ => List.empty)

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
      post = _ => List(Validator.hasPositiveRenown)
    )

  private val completeQuestValidator: EffectValidator[CompleteQuest] =
    from(
      pre = effect => List(Validator.questIncomplete(effect.quest)),
      post = _ => List.empty
    )

  private val completeTaskValidator: EffectValidator[CompleteTask] =
    from(
      pre = effect => List(Validator.taskIncomplete(effect.task)),
      post = _ => List.empty
    )

  private def from[E <: Effect](
    pre: E => List[Validator],
    post: E => List[Validator]
  ): EffectValidator[E] =
    new EffectValidator[E] {
      def validate(effect: E)(player: Player, itemCache: ItemCache): (List[String], Player) = {
        val postEffectPlayer = EffectResolver.resolve(player, effect)
        val errors =
          collectErrors(pre(effect))(player, itemCache) ++
            collectErrors(post(effect))(postEffectPlayer, itemCache)

        (errors, postEffectPlayer)
      }

      private def collectErrors(validators: List[Validator])(player: Player, itemCache: ItemCache): List[String] =
        validators
          .map(_.apply(player, itemCache))
          .collect { case Left(error) => error }
    }
}
