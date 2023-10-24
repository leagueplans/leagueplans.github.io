package ddm.common.model

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

sealed trait LeagueTaskArea {
  def name: String
}

object LeagueTaskArea {
  case object Global extends LeagueTaskArea { val name: String = "Global" }
  case object Misthalin extends LeagueTaskArea { val name: String = "Misthalin" }
  case object Karamja extends LeagueTaskArea { val name: String = "Karamja" }
  case object Asgarnia extends LeagueTaskArea { val name: String = "Asgarnia" }
  case object Fremennik extends LeagueTaskArea { val name: String = "Fremennik Provinces" }
  case object Kandarin extends LeagueTaskArea { val name: String = "Kandarin" }
  case object Desert extends LeagueTaskArea { val name: String = "Kharidian Desert" }
  case object Morytania extends LeagueTaskArea { val name: String = "Morytania" }
  case object Tirannwn extends LeagueTaskArea { val name: String = "Tirannwn" }
  case object Wilderness extends LeagueTaskArea { val name: String = "Wilderness" }
  case object Kourend extends LeagueTaskArea { val name: String = "Kourend & Kebos" }

  val all: Set[LeagueTaskArea] =
    Set(Global, Misthalin, Karamja, Asgarnia, Fremennik, Kandarin, Desert, Morytania, Tirannwn, Wilderness, Kourend)

  implicit val codec: Codec[LeagueTaskArea] = deriveCodec[LeagueTaskArea]
}