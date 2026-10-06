package com.leagueplans.ui.model.player

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.player.item.{BankSpace, Depository}
import com.leagueplans.ui.model.player.league.LeagueStatus
import com.leagueplans.ui.model.player.skill.Stats

object Player {
  private final case class Simplified(
    stats: Stats,
    depositories: Iterable[Depository],
    completedQuests: Set[Int],
    completedDiaryTasks: Set[Int],
    leagueStatus: LeagueStatus,
    gridStatus: GridStatus,
    // Optional so that players encoded before there was bank space still decode
    bankSpace: Option[BankSpace]
  )

  given Encoder[Player] = Encoder.derived[Simplified].contramap(player =>
    Simplified(
      player.stats,
      player.depositories.values,
      player.completedQuests,
      player.completedDiaryTasks,
      player.leagueStatus,
      player.gridStatus,
      Some(player.bankSpace)
    )
  )

  given Decoder[Player] = Decoder.derived[Simplified].map(simplified =>
    Player(
      simplified.stats,
      simplified.depositories.map(d => d.kind -> d).toMap,
      simplified.completedQuests,
      simplified.completedDiaryTasks,
      simplified.leagueStatus,
      simplified.gridStatus,
      simplified.bankSpace.getOrElse(BankSpace.none)
    )
  )
}

final case class Player(
  stats: Stats,
  depositories: Map[Depository.Kind, Depository],
  completedQuests: Set[Int],
  completedDiaryTasks: Set[Int],
  leagueStatus: LeagueStatus,
  gridStatus: GridStatus,
  bankSpace: BankSpace = BankSpace.none
) {
  def get(kind: Depository.Kind): Depository =
    depositories.getOrElse(kind, Depository.empty(kind))

  /** How many slots a place has, which for the bank depends on what's been unlocked */
  def capacity(kind: Depository.Kind): Int =
    kind match {
      case Depository.Kind.Bank => bankSpace.capacity
      case other => other.capacity
    }
}
