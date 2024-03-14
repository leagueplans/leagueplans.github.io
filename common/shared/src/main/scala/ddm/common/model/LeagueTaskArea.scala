package ddm.common.model

import io.circe.{Decoder, Encoder}

enum LeagueTaskArea(val name: String) {
  case Global extends LeagueTaskArea("Global")
  case Misthalin extends LeagueTaskArea("Misthalin")
  case Karamja extends LeagueTaskArea("Karamja")
  case Asgarnia extends LeagueTaskArea("Asgarnia")
  case Fremennik extends LeagueTaskArea("Fremennik Provinces")
  case Kandarin extends LeagueTaskArea("Kandarin")
  case Desert extends LeagueTaskArea("Kharidian Desert")
  case Kourend extends LeagueTaskArea("Kourend & Kebos")
  case Morytania extends LeagueTaskArea("Morytania")
  case Tirannwn extends LeagueTaskArea("Tirannwn")
  case Wilderness extends LeagueTaskArea("Wilderness")
}

object LeagueTaskArea {
  private val nameToArea: Map[String, LeagueTaskArea] =
    values.map(area => area.name -> area).toMap

  given Encoder[LeagueTaskArea] = Encoder[String].contramap(_.name)
  given Decoder[LeagueTaskArea] = Decoder[String].emap(s =>
    nameToArea
      .get(s)
      .toRight(left = s"Unknown area name: [$s]")
  )
}
