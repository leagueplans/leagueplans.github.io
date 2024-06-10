package ddm.ui.model.player.mode

import ddm.codec.decoding.{Decoder, DecodingFailure}
import ddm.codec.encoding.Encoder
import ddm.ui.model.player.Player

trait Mode {
  def name: String
  def initialPlayer: Player
}

object Mode {
  trait League extends Mode

  object League {
    val all: List[League] =
      List(LeaguesI, LeaguesII, LeaguesIII, LeaguesIV)
  }

  val all: List[Mode] =
    MainGame +: League.all

  given Encoder[Mode] =
    Encoder[String].contramap {
      case MainGame => "main-game"
      case LeaguesI => "leagues-1"
      case LeaguesII => "leagues-2"
      case LeaguesIII => "leagues-3"
      case LeaguesIV => "leagues-4"
      case other => throw IllegalArgumentException(s"Could not encode unknown game mode: [$other]")
    }

  given Decoder[Mode] =
    Decoder[String].emap {
      case "main-game" => Right(MainGame)
      case "leagues-1" => Right(LeaguesI)
      case "leagues-2" => Right(LeaguesII)
      case "leagues-3" => Right(LeaguesIII)
      case "leagues-4" => Right(LeaguesIV)
      case other => Left(DecodingFailure(s"Could not decode unknown game mode: [$other]"))
    }
}
