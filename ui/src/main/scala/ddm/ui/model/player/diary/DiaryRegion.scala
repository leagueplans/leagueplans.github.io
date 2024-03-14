package ddm.ui.model.player.diary

import io.circe.{Decoder, Encoder}

enum DiaryRegion(val name: String) {
  case Ardougne extends DiaryRegion("Ardougne")
  case Desert extends DiaryRegion("Desert")
  case Falador extends DiaryRegion("Falador")
  case Fremennik extends DiaryRegion("Fremennik")
  case Kandarin extends DiaryRegion("Kandarin")
  case Karamja extends DiaryRegion("Karamja")
  case Kourend extends DiaryRegion("Kourend & Kebos")
  case Lumbridge extends DiaryRegion("Lumbridge & Draynor")
  case Morytania extends DiaryRegion("Morytania")
  case Varrock extends DiaryRegion("Varrock")
  case WesternProvinces extends DiaryRegion("Western Provinces")
  case Wilderness extends DiaryRegion("Wilderness")
}

object DiaryRegion {
  private val nameToRegion: Map[String, DiaryRegion] =
    values.map(s => s.toString -> s).toMap

  given Encoder[DiaryRegion] = Encoder[String].contramap(_.toString)
  given Decoder[DiaryRegion] = Decoder[String].emap(s =>
    nameToRegion
      .get(s)
      .toRight(left = s"Unknown diary region: [$s]")
  )

  given Ordering[DiaryRegion] =
    Ordering.by(_.name)
}
