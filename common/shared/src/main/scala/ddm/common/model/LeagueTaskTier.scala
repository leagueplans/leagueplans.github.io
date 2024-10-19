package ddm.common.model

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import io.circe.{Decoder as JsonDecoder, Encoder as JsonEncoder}

import scala.util.Try

enum LeagueTaskTier {
  case Beginner, Easy, Medium, Hard, Elite, Master
}

object LeagueTaskTier {
  given JsonEncoder[LeagueTaskTier] = JsonEncoder[String].contramap(_.toString)
  given JsonDecoder[LeagueTaskTier] = JsonDecoder[String].emapTry(s =>
    Try(LeagueTaskTier.valueOf(s))
  )

  given Encoder[LeagueTaskTier] = Encoder.derived
  given Decoder[LeagueTaskTier] = Decoder.derived
}
