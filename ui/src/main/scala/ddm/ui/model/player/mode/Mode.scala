package ddm.ui.model.player.mode

import ddm.ui.model.player.Player
import ddm.ui.model.player.league.ExpMultiplierStrategy
import io.circe.{Decoder, Encoder}

trait Mode {
  def name: String
  def initialPlayer: Player
  def expMultiplierStrategy: ExpMultiplierStrategy
}

object Mode {
  trait League extends Mode

  object League {
    val all: List[League] =
      List(LeaguesI, LeaguesII, LeaguesIII, LeaguesIV)
  }

  val all: List[Mode] =
    MainGame +: League.all

  implicit val encoder: Encoder[Mode] =
    Encoder[String].contramap {
      case MainGame => "main-game"
      case LeaguesI => "leagues-1"
      case LeaguesII => "leagues-2"
      case LeaguesIII => "leagues-3"
      case LeaguesIV => "leagues-4"
      case other => throw new IllegalArgumentException(s"Could not encode unknown game mode: [$other]")
    }

  implicit val decoder: Decoder[Mode] =
    Decoder[String].emap {
      case "main-game" => Right(MainGame)
      case "leagues-1" => Right(LeaguesI)
      case "leagues-2" => Right(LeaguesII)
      case "leagues-3" => Right(LeaguesIII)
      case "leagues-4" => Right(LeaguesIV)
      case other => Left(s"Could not decode unknown game mode: [$other]")
    }
}
