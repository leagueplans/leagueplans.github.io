package ddm.ui.model.plan

import io.circe.syntax.EncoderOps
import io.circe.{Decoder, Encoder, JsonObject}

import java.util.UUID
import scala.annotation.tailrec

object Step {
  def apply(
    description: String,
    directEffects: List[Effect],
    substeps: List[Step]
  ): Step =
    Step(
      UUID.randomUUID(),
      description,
      directEffects,
      substeps
    )

  implicit val encoder: Encoder[Step] =
    Encoder[JsonObject].contramap(step =>
      JsonObject(
        "description" -> step.description.asJson,
        "directEffects" -> step.directEffects.asJson,
        "substeps" -> step.substeps.asJson
      )
    )

  implicit val decoder: Decoder[Step] =
    Decoder[JsonObject].emap(obj =>
      for {
        description <- decodeField[String](obj, "description")
        directEffects <- decodeField[List[Effect]](obj, "directEffects")
        substeps <- decodeField[List[Step]](obj, "substeps")
      } yield Step(description, directEffects, substeps)
    )

  private def decodeField[T : Decoder](obj: JsonObject, key: String): Either[String, T] =
    obj(key)
      .toRight(left = s"Missing key: [$key]")
      .flatMap(_.as[T].left.map(_.message))
}

final case class Step(
  id: UUID,
  description: String,
  directEffects: List[Effect],
  substeps: List[Step]
) {
  lazy val flattened: List[Step] =
    flatten(acc = List.empty, remaining = List(this))

  def takeUntil(lastID: UUID): List[Step] = {
    val (lhs, rhs) = flattened.span(_.id != lastID)
    lhs ++ rhs.headOption
  }

  @tailrec
  private def flatten(acc: List[Step], remaining: List[Step]): List[Step] =
    remaining match {
      case Nil => acc
      case h :: t => flatten(acc = acc :+ h, remaining = h.substeps ++ t)
    }
}
