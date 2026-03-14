package com.leagueplans.ui.projection.worker

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.projection.model.Projection

object ProjectionProtocol {
  sealed trait Inbound {
    def id: Long
  }

  object Inbound {
    final case class Initialise(
      id: Long,
      plan: Forest[Step.ID, Step],
      settings: Plan.Settings
    ) extends Inbound

    final case class ForestUpdated(
      id: Long,
      update: Forest.Update[Step.ID, Step]
    ) extends Inbound

    final case class SettingsChanged(
      id: Long,
      settings: Plan.Settings
    ) extends Inbound

    final case class FocusChanged(
      id: Long,
      focusID: Option[Step.ID]
    ) extends Inbound

    given Encoder[Inbound] = Encoder.derived
    given Decoder[Inbound] = Decoder.derived
  }

  sealed trait Outbound

  object Outbound {
    final case class Computed(id: Long, result: Projection) extends Outbound
    final case class ComputeFailed(id: Long, message: String) extends Outbound

    given Encoder[Outbound] = Encoder.derived
    given Decoder[Outbound] = Decoder.derived
  }
}
