package com.leagueplans.ui.model.player.item

import com.leagueplans.ui.model.plan.Step

/** Builds a RuneLite bank tag tab holding a set of stacks, laid out as they are in the
  * inventory, which the Bank Tags plugin can import.
  *
  * RuneLite's parsing logic for bank tags:
  * https://github.com/runelite/runelite/blob/65ae77c168b34e161021239a460e9f4913402661/runelite-client/src/main/java/net/runelite/client/plugins/banktags/tabs/TabInterface.java#L525
  */
object BankTags {
  private val formatVersion = 1
  /** The tab's icon */
  private val newcomerMapID = 550

  /** A tab's name, unique to the step it's copied for, since importing a tab replaces any tab of
    * the same name. The start of the step's ID is enough to tell a plan's steps apart. Without a
    * step, the tab is just "leagueplans".
    */
  def tabName(step: Option[Step.ID]): String =
    step.fold("leagueplans")(id => s"lp-${id.filter(_.isLetterOrDigit).take(6).toLowerCase}")

  def layout(name: String, stacks: List[ItemStack]): String = {
    val positions = Iterator.iterate(0)(increment)
    val entries =
      stacks.iterator.zip(positions).flatMap((stack, index) => stack.item.gameID.map(id => s",$index,$id")).mkString
    s"banktags,$formatVersion,$name,$newcomerMapID,layout$entries"
  }

  /** The bank is 8 slots wide and the inventory 4, so each inventory row starts a new bank row */
  private def increment(index: Int): Int =
    if (index % 8 == 3) index + 5 else index + 1
}
