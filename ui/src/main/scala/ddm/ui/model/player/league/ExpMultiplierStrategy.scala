package ddm.ui.model.player.league

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
}
