package ddm.ui.model

import ddm.common.model.Item
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.{CompleteQuest, CompleteTask, DropItem, GainExp, GainItem, MoveItem, SetMultiplier, UnlockSkill}
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.model.player.skill.Skill

sealed trait EffectValidator[E <: Effect] {
  def validate(effect: E)(player: Player, itemCache: ItemCache): (List[String], Player)
}

object EffectValidator extends EffectValidator[Effect] {
  def validate(effect: Effect)(player: Player, itemCache: ItemCache): (List[String], Player) =
    effect match {
      case e: GainExp => gainExpValidator.validate(e)(player, itemCache)
      case e: GainItem => gainItemValidator.validate(e)(player, itemCache)
      case e: MoveItem => moveItemValidator.validate(e)(player, itemCache)
      case e: DropItem => dropItemValidator.validate(e)(player, itemCache)
      case e: UnlockSkill => unlockSkillValidator.validate(e)(player, itemCache)
      case e: CompleteQuest => empty.validate(e)(player, itemCache)
      case e: SetMultiplier => empty.validate(e)(player, itemCache)
      case e: CompleteTask => empty.validate(e)(player, itemCache)
    }

  private def empty[E <: Effect]: EffectValidator[E] =
    from(_ => List.empty, _ => List.empty)

  private val gainExpValidator: EffectValidator[GainExp] =
    from(
      pre = gain => List(Validator.skillUnlocked(gain.skill)),
      post = _ => List.empty
    )

  private val gainItemValidator: EffectValidator[GainItem] =
    from(
      pre = _ => List.empty,
      post = gain => List(Validator.depositorySize(gain.target))
    )

  private val moveItemValidator: EffectValidator[MoveItem] =
    from(
      pre = move => List(Validator.hasItem(move.source, move.item, move.count)),
      post = move => List(Validator.depositorySize(move.target))
    )

  private val dropItemValidator: EffectValidator[DropItem] =
    from(
      pre = drop => List(Validator.hasItem(drop.source, drop.item, drop.count)),
      post = _ => List.empty
    )

  private val unlockSkillValidator: EffectValidator[UnlockSkill] =
    from(
      pre = _ => List.empty,
      post = _ => List(Validator.hasPositiveRenown)
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
          .map(_.validate(player, itemCache))
          .collect { case Left(error) => error }
    }

  private final case class Validator(validate: (Player, ItemCache) => Either[String, Unit])

  private object Validator {
    def depositorySize(depositoryID: Depository.ID): Validator =
      Validator(
        (player, itemCache) => {
          val depository = player.depositories(depositoryID)
          val size = itemCache.itemise(depository).size
          Either.cond(
            size <= depository.capacity,
            right = (),
            left = s"${depository.id.raw} requires $size spaces (max ${depository.capacity})"
          )
        }
      )

    def hasItem(depositoryID: Depository.ID, itemID: Item.ID, removalCount: Int): Validator =
      Validator(
        (player, itemCache) => {
          val heldCount = player.depositories(depositoryID).contents.getOrElse(itemID, 0)
          Either.cond(
            heldCount >= removalCount,
            right = (),
            left = s"${depositoryID.raw} does not have enough of ${itemCache(itemID).name}"
          )
        }
      )

    def skillUnlocked(skill: Skill): Validator =
      Validator(
        (player, _) => Either.cond(
          player.leagueStatus.skillsUnlocked.contains(skill),
          right = (),
          left = s"$skill has not been unlocked yet"
        )
      )

    val hasPositiveRenown: Validator =
      Validator(
        (player, _) => Either.cond(
          player.leagueStatus.expectedRenown >= 0,
          right = (),
          left = s"Not enough renown to make that purchase"
        )
      )
  }
}
