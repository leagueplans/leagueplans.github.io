package ddm.ui.model.player.diary

import io.circe.{Decoder, Encoder}

object DiaryTier {
  case object Easy extends DiaryTier
  case object Medium extends DiaryTier
  case object Hard extends DiaryTier
  case object Elite extends DiaryTier

  val all: List[DiaryTier] =
    List(Easy, Medium, Hard, Elite)

  private val nameToTier: Map[String, DiaryTier] =
    all.map(s => s.toString -> s).toMap

  implicit val encoder: Encoder[DiaryTier] = Encoder[String].contramap(_.toString)
  implicit val decoder: Decoder[DiaryTier] = Decoder[String].emap(s =>
    nameToTier
      .get(s)
      .toRight(left = s"Unknown diary tier: [$s]")
  )

  implicit val ordering: Ordering[DiaryTier] =
    Ordering.by {
      case Easy => 1
      case Medium => 2
      case Hard => 3
      case Elite => 4
    }
}

sealed trait DiaryTier
