package com.leagueplans.ui.model.plan

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.player.Player
import com.leagueplans.ui.model.player.mode.Mode

object Plan {
  given Encoder[Plan] = Encoder.derived
  given Decoder[Plan] = Decoder.derived
  
  sealed trait Settings {
    def initialPlayer: Player
    def expMultipliers: List[ExpMultiplier]
    def maybeLeaguePointScoring: Option[LeaguePointScoring]
  }
  
  object Settings {
    final case class Deferred(mode: Mode) extends Settings {
      val initialPlayer: Player =
        mode.settings.initialPlayer
        
      val expMultipliers: List[ExpMultiplier] =
        mode.settings.expMultipliers
        
      val maybeLeaguePointScoring: Option[LeaguePointScoring] = 
        mode.settings.maybeLeaguePointScoring
    }
    
    final case class Explicit(
      initialPlayer: Player,
      expMultipliers: List[ExpMultiplier],
      maybeLeaguePointScoring: Option[LeaguePointScoring]
    ) extends Settings
    
    given Encoder[Settings] = Encoder.derived
    given Decoder[Settings] = Decoder.derived
  }
}

final case class Plan(
  name: String,
  steps: Forest[Step.ID, Step],
  settings: Plan.Settings
)
