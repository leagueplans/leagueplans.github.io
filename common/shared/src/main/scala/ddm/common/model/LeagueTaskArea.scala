package ddm.common.model

import io.circe.{Decoder, Encoder}

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
  case object Kourend extends LeagueTaskArea { val name: String = "Kourend & Kebos" }
  case object Morytania extends LeagueTaskArea { val name: String = "Morytania" }
  case object Tirannwn extends LeagueTaskArea { val name: String = "Tirannwn" }
  case object Wilderness extends LeagueTaskArea { val name: String = "Wilderness" }

  val all: List[LeagueTaskArea] =
    List(Global, Misthalin, Karamja, Asgarnia, Fremennik, Kandarin, Desert, Kourend, Morytania, Tirannwn, Wilderness)

  private val nameToArea: Map[String, LeagueTaskArea] =
    all.map(area => area.name -> area).toMap

  implicit val encoder: Encoder[LeagueTaskArea] = Encoder[String].contramap(_.name)
  implicit val decoder: Decoder[LeagueTaskArea] = Decoder[String].emap(s =>
    nameToArea
      .get(s)
      .toRight(left = s"Unknown area name: [$s]")
  )
}
