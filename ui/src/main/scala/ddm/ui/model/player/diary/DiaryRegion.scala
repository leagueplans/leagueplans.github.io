package ddm.ui.model.player.diary

import io.circe.{Decoder, Encoder}

object DiaryRegion {
  case object Ardougne extends DiaryRegion { val name: String = "Ardougne" }
  case object Desert extends DiaryRegion { val name: String = "Desert" }
  case object Falador extends DiaryRegion { val name: String = "Falador" }
  case object Fremennik extends DiaryRegion { val name: String = "Fremennik" }
  case object Kandarin extends DiaryRegion { val name: String = "Kandarin" }
  case object Karamja extends DiaryRegion { val name: String = "Karamja" }
  case object Kourend extends DiaryRegion { val name: String = "Kourend & Kebos" }
  case object Lumbridge extends DiaryRegion { val name: String = "Lumbridge & Draynor" }
  case object Morytania extends DiaryRegion { val name: String = "Morytania" }
  case object Varrock extends DiaryRegion { val name: String = "Varrock" }
  case object WesternProvinces extends DiaryRegion { val name: String = "Western Provinces" }
  case object Wilderness extends DiaryRegion { val name: String = "Wilderness" }

  val all: List[DiaryRegion] =
    List(Ardougne, Desert, Falador, Fremennik, Kandarin, Karamja, Kourend,
      Lumbridge, Morytania, Varrock, WesternProvinces, Wilderness)

  private val nameToRegion: Map[String, DiaryRegion] =
    all.map(s => s.toString -> s).toMap

  implicit val encoder: Encoder[DiaryRegion] = Encoder[String].contramap(_.toString)
  implicit val decoder: Decoder[DiaryRegion] = Decoder[String].emap(s =>
    nameToRegion
      .get(s)
      .toRight(left = s"Unknown diary region: [$s]")
  )

  implicit val ordering: Ordering[DiaryRegion] =
    Ordering.by(_.name)
}

sealed trait DiaryRegion {
  def name: String
}
