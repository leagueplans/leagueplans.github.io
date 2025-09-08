package com.leagueplans.ui.model.player.mode

import com.leagueplans.codec.decoding.{Decoder, DecodingFailure}
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.plan.Plan

trait Mode {
  def name: String
  def settings: Plan.Settings.Explicit
}

object Mode {
  trait League extends Mode

  object League {
    val all: List[League] =
      List(LeaguesI, LeaguesII, LeaguesIII, LeaguesIV, LeaguesV)

    given Encoder[League] =
      Encoder[String].contramap {
        case LeaguesI => "leagues-1"
        case LeaguesII => "leagues-2"
        case LeaguesIII => "leagues-3"
        case LeaguesIV => "leagues-4"
        case LeaguesV => "leagues-5"
        case other => throw IllegalArgumentException(s"Could not encode unexpected league: [$other]")
      }

    given Decoder[League] =
      Decoder[String].emap {
        case "leagues-1" => Right(LeaguesI)
        case "leagues-2" => Right(LeaguesII)
        case "leagues-3" => Right(LeaguesIII)
        case "leagues-4" => Right(LeaguesIV)
        case "leagues-5" => Right(LeaguesV)
        case other => Left(DecodingFailure(s"Could not decode unexpected league: [$other]"))
      }
  }

  trait Deadman extends Mode

  object Deadman {
    val all: List[Deadman] =
      List(Armageddon)
  }

  val all: List[Mode] =
    (MainGame +: League.all) ++ Deadman.all
    
  given Encoder[Mode] =
    Encoder[String].contramap {
      case MainGame => "main-game"
      case LeaguesI => "leagues-1"
      case LeaguesII => "leagues-2"
      case LeaguesIII => "leagues-3"
      case LeaguesIV => "leagues-4"
      case LeaguesV => "leagues-5"
      case Armageddon => "deadman-armageddon"
      case other => throw IllegalArgumentException(s"Could not encode unexpected mode: [$other]")
    }

  given Decoder[Mode] =
    Decoder[String].emap {
      case "main-game" => Right(MainGame)
      case "leagues-1" => Right(LeaguesI)
      case "leagues-2" => Right(LeaguesII)
      case "leagues-3" => Right(LeaguesIII)
      case "leagues-4" => Right(LeaguesIV)
      case "leagues-5" => Right(LeaguesV)
      case "deadman-armageddon" => Right(Armageddon)
      case other => Left(DecodingFailure(s"Could not decode unexpected mode: [$other]"))
    }
}
