package com.leagueplans.ui.dom.planning.details

import com.raquo.airstream.core.EventStream
import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.{StrictSignal, Var}

object RowSelection {
  /** Marks the rows that can be selected, so that keyboard shortcuts can tell them apart */
  val rowAttribute: String = "data-selectable-row"

  enum Kind {
    case Effects, Requirements
  }

  final case class Row(kind: Kind, index: Int)

  enum Command {
    case Delete, EditAmount
  }
}

/** The effect or requirement row selected in the step details. While a row is selected, the
  * page's shortcuts for deleting and editing act on it instead of on the focused step.
  */
final class RowSelection {
  import RowSelection.*

  // Kept outside the Var so that a selection made earlier in the same transaction is visible
  private var current = Option.empty[Row]
  private val selectedVar = Var(current)
  private val commandBus = EventBus[Command]()

  val selected: StrictSignal[Option[Row]] = selectedVar.signal
  val commands: EventStream[Command] = commandBus.events

  def select(row: Option[Row]): Unit = {
    current = row
    selectedVar.set(row)
  }

  def isSelected: Boolean =
    current.nonEmpty

  /** Sends a command to the selected row, returning whether there was one to send it to */
  def send(command: Command): Boolean = {
    if (current.nonEmpty) commandBus.emit(command)
    current.nonEmpty
  }
}
