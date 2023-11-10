package ddm.ui.model.player.league

sealed trait ExpMultiplierStrategy

object ExpMultiplierStrategy {
  final case class Fixed(multiplier: Int) extends ExpMultiplierStrategy
  final case class LeaguePointBased(base: Int, thresholds: List[(Int, Int)]) extends ExpMultiplierStrategy
}
