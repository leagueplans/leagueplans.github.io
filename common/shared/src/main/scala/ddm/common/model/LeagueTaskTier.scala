package ddm.common.model

import io.circe.{Decoder, Encoder}

sealed trait LeagueTaskTier

object LeagueTaskTier {
  case object Beginner extends LeagueTaskTier
  case object Easy extends LeagueTaskTier
  case object Medium extends LeagueTaskTier
  case object Hard extends LeagueTaskTier
  case object Elite extends LeagueTaskTier
  case object Master extends LeagueTaskTier

  val all: Set[LeagueTaskTier] =
    Set(Beginner, Easy, Medium, Hard, Elite, Master)

  private val nameToTier: Map[String, LeagueTaskTier] =
    all.map(tier => tier.toString -> tier).toMap

  implicit val encoder: Encoder[LeagueTaskTier] = Encoder[String].contramap(_.toString)
  implicit val decoder: Decoder[LeagueTaskTier] = Decoder[String].emap(s =>
    nameToTier
      .get(s)
      .toRight(left = s"Unknown tier name: [$s]")
  )
}