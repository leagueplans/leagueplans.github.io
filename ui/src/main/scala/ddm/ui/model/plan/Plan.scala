package ddm.ui.model.plan

import ddm.ui.model.common.forest.Forest
import ddm.ui.model.player.mode.Mode
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import java.util.UUID
import scala.annotation.nowarn

object Plan {
  implicit val codec: Codec[Plan] = {
    @nowarn("msg=local val stepsCodec in value codec is never used")
    implicit val stepsCodec: Codec[Forest[UUID, Step]] = Forest.codec(_.id)
    deriveCodec[Plan]
  }

  final case class Named(name: String, plan: Plan)
}

final case class Plan(mode: Mode, steps: Forest[UUID, Step])
