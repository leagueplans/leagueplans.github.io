package ddm.ui.model

import ddm.common.model.Item
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect._
import ddm.ui.model.player.{Player, Quest}
import ddm.ui.model.player.item.{Depository, ItemCache}
import ddm.ui.model.player.league.Task
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

  private val gainItemValidator: EffectValidator[GainItem] =
    from(
      pre = gain => if (gain.count < 0) List(Validator.hasItem(gain.target, gain.item, -gain.count)) else List.empty,
      post = gain => if (gain.count > 0) List(Validator.depositorySize(gain.target)) else List.empty
    )

  private val moveItemValidator: EffectValidator[MoveItem] =
    from(
      pre = move => List(Validator.hasItem(move.source, move.item, move.count)),
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
          .map(_.validate(player, itemCache))
          .collect { case Left(error) => error }
    }

  private final case class Validator(validate: (Player, ItemCache) => Either[String, Unit])

  private object Validator {
    def depositorySize(kind: Depository.Kind): Validator =
      Validator(
        (player, itemCache) => {
          val size = itemCache.itemise(player.get(kind)).map { case (_, stacks) => stacks.size }.sum
          Either.cond(
            size <= kind.capacity,
            right = (),
            left = s"${kind.name} requires $size spaces (max ${kind.capacity})"
          )
        }
      )

    def hasItem(kind: Depository.Kind, itemID: Item.ID, removalCount: Int): Validator =
      Validator(
        (player, itemCache) => {
          val heldCount = player.get(kind).contents.getOrElse(itemID, 0)
          Either.cond(
            heldCount >= removalCount,
            right = (),
            left = s"${kind.name} does not have enough of ${itemCache(itemID).name}"
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

    def questIncomplete(quest: Quest): Validator =
      Validator(
        (player, _) => Either.cond(
          !player.completedQuests.contains(quest),
          right = (),
          left = s"${quest.name} has already been completed"
        )
      )

    def taskIncomplete(task: Task): Validator =
      Validator(
        (player, _) => Either.cond(
          !player.leagueStatus.tasksCompleted.contains(task),
          right = (),
          left = s"${task.name} has already been completed"
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
