package com.leagueplans.ui.storage.local

import com.leagueplans.ui.model.player.ViewerAccount

/** Remembers what's true of the viewer's account in this browser, for every plan they open. If
  * it can't be read, the account is assumed to be as most are. */
object ViewerAccountStorage {
  private val key = "viewer-account"
  private val jagexAccount = "jagex-account"
  private val authenticator = "authenticator"

  def load(): ViewerAccount =
    LocalStorage.get(key).fold(ViewerAccount.default) { value =>
      val flags = value.split(',').toSet
      ViewerAccount(jagexAccount = flags.contains(jagexAccount), authenticator = flags.contains(authenticator))
    }

  def save(account: ViewerAccount): Unit =
    LocalStorage.set(
      key,
      List(Option.when(account.jagexAccount)(jagexAccount), Option.when(account.authenticator)(authenticator)).flatten.mkString(",")
    )
}
