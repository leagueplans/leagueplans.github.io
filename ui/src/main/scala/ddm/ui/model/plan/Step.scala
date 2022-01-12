package ddm.ui.model.plan

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto.{deriveDecoder, deriveEncoder}

object Step {
  implicit val decoder: Decoder[Step] = deriveDecoder[Step]
  implicit val encoder: Encoder[Step] = deriveEncoder[Step]
}

final case class Step(
  description: String,
  directEffects: List[Effect],
  substeps: List[Step]
) {
  lazy val allEffects: List[Effect] =
    directEffects ++ substeps.flatMap(_.allEffects)
}
