package com.leagueplans.ui.dom.player.task

import com.leagueplans.ui.dom.common.{Button, IconButtonModifiers}
import com.leagueplans.ui.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.ui.utils.laminar.FontAwesome
import com.leagueplans.ui.utils.laminar.LaminarOps.handled
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskPanel {
  def apply(
    title: L.HtmlElement,
    toSummaryTab: Observer[Unit] => L.HtmlElement,
    detailsTab: L.HtmlElement
  ): L.Div = {
    val tabVar = Var[Tab](Tab.Summary)
    val summaryTab = toSummaryTab(tabVar.writer.contramap[Unit](_ => Tab.Details))

    L.div(
      L.cls(Styles.panel, PanelStyles.panel),
      L.div(
        L.cls(Styles.header),
        toTabToggle(tabVar),
        title.amend(L.cls(Styles.title, PanelStyles.header))
      ),
      L.child <-- tabVar.signal.map {
        case Tab.Summary => summaryTab.amend(L.cls(Styles.tab))
        case Tab.Details => detailsTab.amend(L.cls(Styles.tab))
      }
    )
  }

  private enum Tab {
    case Summary, Details
  }

  @js.native @JSImport("/styles/player/task/taskPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
    val tabToggle: String = js.native
    val title: String = js.native
    val tab: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def toTabToggle(tabVar: Var[Tab]): L.Button =
    Button(_.handled --> tabVar.updater {
      case (Tab.Summary, _) => Tab.Details
      case (Tab.Details, _) => Tab.Summary
    }).amend(
      L.cls(Styles.tabToggle),
      L.child <-- tabVar.signal.map {
        case Tab.Summary => FontAwesome.icon(FreeSolid.faBars)
        case Tab.Details => FontAwesome.icon(FreeSolid.faHouse)
      },
      IconButtonModifiers(
        tooltip = tabVar.signal.map {
          case Tab.Summary => "Switch to the detailed view"
          case Tab.Details => "Switch to the summary view"
        },
        screenReaderDescription = tabVar.signal.map {
          case Tab.Summary => "detailed view"
          case Tab.Details => "summary view"
        }
      )
    )
}
