package com.leagueplans.ui.model.validation

import com.leagueplans.ui.model.plan.Requirement
import com.leagueplans.ui.model.plan.Requirement.*
import com.leagueplans.ui.model.player.mode.Mode
import com.leagueplans.ui.model.player.{Cache, Player}

sealed trait RequirementValidator[R <: Requirement] {
  def validate(requirement: R)(player: Player, league: Option[Mode.League], cache: Cache): List[String]
}

object RequirementValidator {
  def validate(requirements: List[Requirement])(
    player: Player,
    league: Option[Mode.League],
    cache: Cache
  ): List[String] =
    requirements.flatMap(validate(_)(player, league, cache))

  def validate(requirement: Requirement)(
    player: Player,
    league: Option[Mode.League],
    cache: Cache
  ): List[String] =
    requirement match {
      case r: SkillLevel => levelValidator.validate(r)(player, league, cache)
      case r: Tool => toolValidator.validate(r)(player, league, cache)
      case r: And => andValidator.validate(r)(player, league, cache)
      case r: Or => orValidator.validate(r)(player, league, cache)
    }

  private val levelValidator: RequirementValidator[SkillLevel] =
    new RequirementValidator[SkillLevel] {
      def validate(requirement: SkillLevel)(
        player: Player,
        league: Option[Mode.League],
        cache: Cache
      ): List[String] =
        Validator.hasLevel(requirement.skill, requirement.level)(player, league, cache) match {
          case Left(error) => List(error)
          case Right(()) => List.empty
        }
    }

  private val toolValidator: RequirementValidator[Tool] =
    new RequirementValidator[Tool] {
      def validate(requirement: Tool)(
        player: Player,
        league: Option[Mode.League],
        cache: Cache
      ): List[String] =
        Validator.hasItem(
          requirement.location,
          requirement.item,
          noted = false,
          requiredCount = 1
        )(player, league, cache) match {
          case Left(error) => List(error)
          case Right(()) => List.empty
        }
    }

  private val andValidator: RequirementValidator[And] =
    new RequirementValidator[And] {
      def validate(requirement: And)(
        player: Player,
        league: Option[Mode.League],
        cache: Cache
      ): List[String] =
        RequirementValidator.validate(requirement.left)(player, league, cache) ++
          RequirementValidator.validate(requirement.right)(player, league, cache)
    }

  private val orValidator: RequirementValidator[Or] =
    new RequirementValidator[Or] {
      def validate(requirement: Or)(
        player: Player,
        league: Option[Mode.League],
        cache: Cache
      ): List[String] =
        RequirementValidator.validate(requirement.left)(player, league, cache) match {
          case Nil => List.empty
          case leftErrors =>
            RequirementValidator.validate(requirement.right)(player, league, cache) match {
              case Nil => List.empty
              case rightErrors => leftErrors ++ rightErrors
            }
        }
    }
}
