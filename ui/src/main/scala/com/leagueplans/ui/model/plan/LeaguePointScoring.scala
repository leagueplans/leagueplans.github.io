package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.common.model.{LeagueTask, LeagueTaskTier}
import com.leagueplans.ui.model.player.mode.*

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
