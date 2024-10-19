package ddm.ui.model.plan

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder

sealed trait ExpMultiplierStrategy

object ExpMultiplierStrategy {
  final case class Fixed(multiplier: Int) extends ExpMultiplierStrategy

  final case class LeaguePointBased(base: Int, thresholds: List[(Int, Int)]) extends ExpMultiplierStrategy {
    def multiplierAt(leaguePoints: Int): Int =
      thresholds
        .takeWhile { case (pointThreshold, _) => pointThreshold <= leaguePoints }
        .lastOption
        .map { case (_, multiplier) => multiplier }
        .getOrElse(base)
  }

  given Encoder[ExpMultiplierStrategy] = Encoder.derived
  given Decoder[ExpMultiplierStrategy] = Decoder.derived
}
