package com.leagueplans.ui.dom.plans

import com.leagueplans.ui.model.player.mode.{GridMaster, MainGame, Mode}
import com.leagueplans.uicommon.dom.form.Select
import com.leagueplans.uicommon.dom.form.Select.{Group, Opt}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.L

/** A dropdown of the game modes, grouped by the kind of game they're for, newest first */
object ModeSelect {
  def apply(id: String, selection: Var[Mode]): (L.Select, L.Label) =
    Select.grouped(id, groups.map((heading, modes) => Group(Some(heading), modes.map(toOpt))), selection)

  private[plans] val groups: List[(String, List[Mode])] =
    List(
      "Leagues" -> Mode.League.all.reverse,
      "Deadman" -> Mode.Deadman.all.reverse,
      "Main game" -> List(MainGame),
      "Events" -> List(GridMaster)
    )

  private def toOpt(mode: Mode): Opt[Mode] =
    Opt(mode, mode.name)
}
