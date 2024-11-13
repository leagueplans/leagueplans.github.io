package ddm.ui.model.plan

import ddm.codec.decoding.Decoder
import ddm.codec.encoding.Encoder
import ddm.common.model.{LeagueTask, LeagueTaskTier}
import ddm.ui.model.player.mode.*

object LeaguePointScoring {
  given Encoder[LeaguePointScoring] = Encoder.derived
  given Decoder[LeaguePointScoring] = Decoder.derived
}

final case class LeaguePointScoring(
  league: Mode.League,
  tiers: Map[LeagueTaskTier, Int]
) {
  def apply(task: LeagueTask): Int =
    findTier(task).flatMap(tiers.get).getOrElse(0)

  private def findTier(task: LeagueTask): Option[LeagueTaskTier] =
    league match {
      case LeaguesI => task.leagues1Props
      case LeaguesII => task.leagues2Props.map(_.tier)
      case LeaguesIII => task.leagues3Props.map(_.tier)
      case LeaguesIV => task.leagues4Props.map(_.tier)
      case LeaguesV => task.leagues5Props.map(_.tier)
      case _ => None
    }
}
