package ddm.ui.model.validation

import ddm.ui.model.plan.Requirement
import ddm.ui.model.plan.Requirement._
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}

sealed trait RequirementValidator[R <: Requirement] {
  def validate(requirement: R)(player: Player, itemCache: ItemCache): List[String]
}

object RequirementValidator {
  def validate(requirements: List[Requirement])(player: Player, itemCache: ItemCache): List[String] =
    requirements.flatMap(validate(_)(player, itemCache))

  def validate(requirement: Requirement)(player: Player, itemCache: ItemCache): List[String] =
    requirement match {
      case r: Level => levelValidator.validate(r)(player, itemCache)
      case r: Tool => toolValidator.validate(r)(player, itemCache)
      case r: And => andValidator.validate(r)(player, itemCache)
      case r: Or => orValidator.validate(r)(player, itemCache)
    }

  private val levelValidator: RequirementValidator[Level] =
    new RequirementValidator[Level] {
      def validate(requirement: Level)(player: Player, itemCache: ItemCache): List[String] =
        Validator.hasLevel(requirement.skill, requirement.level)(player, itemCache) match {
          case Left(error) => List(error)
          case Right(()) => List.empty
        }
    }

  private val toolValidator: RequirementValidator[Tool] =
    new RequirementValidator[Tool] {
      def validate(requirement: Tool)(player: Player, itemCache: ItemCache): List[String] =
        Validator.hasItem(
          Depository.Kind.Inventory,
          requirement.item,
          noted = false,
          requiredCount = 1
        )(player, itemCache) match {
          case Left(error) => List(error)
          case Right(()) => List.empty
        }
    }

  private val andValidator: RequirementValidator[And] =
    new RequirementValidator[And] {
      def validate(requirement: And)(player: Player, itemCache: ItemCache): List[String] =
        RequirementValidator.validate(requirement.left)(player, itemCache) ++
          RequirementValidator.validate(requirement.right)(player, itemCache)
    }

  private val orValidator: RequirementValidator[Or] =
    new RequirementValidator[Or] {
      def validate(requirement: Or)(player: Player, itemCache: ItemCache): List[String] =
        RequirementValidator.validate(requirement.left)(player, itemCache) match {
          case Nil => List.empty
          case leftErrors =>
            RequirementValidator.validate(requirement.right)(player, itemCache) match {
              case Nil => List.empty
              case rightErrors => leftErrors ++ rightErrors
            }
        }
    }
}
