package com.leagueplans.ui.model.player

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.ui.model.player.item.BankSpace

/** What's true of the account of whoever's looking at a plan, which applies to every plan they
  * open rather than belonging to any of them. Most players have both, so both are assumed until
  * the viewer says otherwise. */
final case class ViewerAccount(jagexAccount: Boolean, authenticator: Boolean) {
  def bankUnlocks: Set[BankSpace.Unlock] =
    Set(
      Option.when(jagexAccount)(BankSpace.Unlock.JagexAccount),
      Option.when(authenticator)(BankSpace.Unlock.Authenticator)
    ).flatten

  /** A player as they'd be with this viewer's account, in place of whatever account they had */
  def applyTo(player: Player): Player =
    player.copy(bankSpace = player.bankSpace.copy(unlocks = player.bankSpace.unlocks.filterNot(_.isAccount) ++ bankUnlocks))
}

object ViewerAccount {
  val default: ViewerAccount = ViewerAccount(jagexAccount = true, authenticator = true)

  given Encoder[ViewerAccount] = Encoder.derived
  given Decoder[ViewerAccount] = Decoder.derived
}
