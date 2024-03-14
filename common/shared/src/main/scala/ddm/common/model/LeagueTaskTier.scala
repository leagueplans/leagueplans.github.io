package ddm.common.model

import io.circe.{Decoder, Encoder}

import scala.util.Try

enum LeagueTaskTier {
  case Beginner, Easy, Medium, Hard, Elite, Master
}

object LeagueTaskTier {
  given Encoder[LeagueTaskTier] = Encoder[String].contramap(_.toString)
  given Decoder[LeagueTaskTier] = Decoder[String].emapTry(s =>
    Try(LeagueTaskTier.valueOf(s))
  )
}
