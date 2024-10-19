package ddm.ui.model.player.mode

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import ddm.ui.model.plan.Plan

trait Mode {
  def name: String
  def settings: Plan.Settings
}

object Mode {
  trait League extends Mode

  object League {
    val all: List[League] =
      List(LeaguesI, LeaguesII, LeaguesIII, LeaguesIV)

    given Encoder[League] =
      Encoder[String].contramap {
        case LeaguesI => "leagues-1"
        case LeaguesII => "leagues-2"
        case LeaguesIII => "leagues-3"
        case LeaguesIV => "leagues-4"
        case other => throw IllegalArgumentException(s"Could not encode unexpected league: [$other]")
      }

    given Decoder[League] =
      Decoder[String].emap {
        case "leagues-1" => Right(LeaguesI)
        case "leagues-2" => Right(LeaguesII)
        case "leagues-3" => Right(LeaguesIII)
        case "leagues-4" => Right(LeaguesIV)
        case other => Left(DecodingFailure(s"Could not decode unexpected league: [$other]"))
      }
  }

  val all: List[Mode] =
    MainGame +: League.all
}
