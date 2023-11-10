package ddm.ui.model.player.league

import ddm.common.model.Skill

final case class LeagueStatus(
  leaguePoints: Int,
  completedTasks: Set[Int],
  skillsUnlocked: Set[Skill]
) {
  def multiplierUsing(strategy: ExpMultiplierStrategy): Int =
    strategy match {
      case ExpMultiplierStrategy.Fixed(multiplier) => multiplier
      case ExpMultiplierStrategy.LeaguePointBased(base, thresholds) =>
        thresholds
          .takeWhile { case (pointThreshold, _) => pointThreshold <= leaguePoints }
          .lastOption
          .map { case (_, multiplier) => multiplier }
          .getOrElse(base)
    }
}
