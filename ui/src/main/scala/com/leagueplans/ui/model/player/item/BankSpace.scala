package com.leagueplans.ui.model.player.item

import com.leagueplans.codec.decoding.Decoder
import com.leagueplans.common.model.Item
import com.leagueplans.codec.encoding.Encoder
import com.leagueplans.uicommon.utils.scala.IntOps.withCommas

/** The bank's slots: the 900 every account has, 20 for each of the account's unlocks, and 50 for
  * each block of space bought from a banker.
  *
  * @param blocksBought how many of the blocks are bought. They're bought in order.
  */
final case class BankSpace(unlocks: Set[BankSpace.Unlock], blocksBought: Int) {
  def capacity: Int =
    BankSpace.base + unlocks.size * BankSpace.unlockSlots + blocksBought * BankSpace.blockSlots

  /** Where the slots come from, as in "900, 40 from your account and 20 from the bank PIN" */
  def breakdown: String = {
    val account = unlocks.count(_.isAccount) * BankSpace.unlockSlots
    val parts =
      Option.when(account > 0)(s"$account from your account") ++
        Option.when(unlocks.contains(BankSpace.Unlock.Pin))(s"${BankSpace.unlockSlots} from the bank PIN") ++
        Option.when(blocksBought > 0)(
          s"${(blocksBought * BankSpace.blockSlots).withCommas} bought"
        )
    parts.toList match {
      case Nil => BankSpace.base.toString
      case List(only) => s"${BankSpace.base}, and $only"
      case several => s"${BankSpace.base}, ${several.init.mkString(", ")} and ${several.last}"
    }
  }
}

object BankSpace {
  /** Each unlock adds 20 slots. A Jagex Account and an authenticator belong to whoever's looking at
    * a plan, so they come from that person's preferences rather than from the plan, while a PIN is
    * set during the plan. */
  enum Unlock(val isAccount: Boolean) {
    case JagexAccount extends Unlock(isAccount = true)
    case Authenticator extends Unlock(isAccount = true)
    case Pin extends Unlock(isAccount = false)
  }

  object Unlock {
    given Encoder[Unlock] = Encoder.derived
    given Decoder[Unlock] = Decoder.derived
  }

  val base: Int = 900
  val unlockSlots: Int = 20
  val blockSlots: Int = 50

  /** What each block costs in coins, in the order they're bought */
  val blockPrices: Vector[Int] =
    Vector(1000000, 2000000, 5000000, 10000000, 20000000, 50000000, 100000000, 200000000, 500000000)

  /** What a block costs, numbering them from 1, if there is such a block */
  def price(block: Int): Option[Int] =
    blockPrices.lift(block - 1)

  val coins: Item.ID = Item.ID(2651) // Coins

  val none: BankSpace = BankSpace(Set.empty, blocksBought = 0)

  given Encoder[BankSpace] = Encoder.derived
  given Decoder[BankSpace] = Decoder.derived
}
