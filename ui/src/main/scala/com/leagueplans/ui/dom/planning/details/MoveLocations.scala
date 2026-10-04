package com.leagueplans.ui.dom.planning.details

import com.leagueplans.common.model.Item
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.player.item.ItemRoute
import com.leagueplans.uicommon.dom.{Button, ContextMenu, ContextMenuList}
import com.leagueplans.uicommon.facades.fontawesome.freesolid.FreeSolid
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.{handled, handledWith}
import com.leagueplans.uicommon.utils.laminar.FontAwesome
import com.raquo.airstream.core.Observer
import com.raquo.laminar.api.{L, textToTextNode}
import org.scalajs.dom.{Element, Event, window}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** The places an item move goes from and to. Either opens a menu of the routes the item can
  * take, which change both ends at once, since only some pairs of places make sense together. */
object MoveLocations {
  /** @param onChange told about the move with a changed route */
  def apply(
    move: Effect.MoveItem,
    item: Item,
    contextMenu: ContextMenu,
    onChange: Observer[Effect.MoveItem]
  ): L.Span = {
    val current = ItemRoute.of(move)
    val openMenu = Observer[Event] { event =>
      val rect = event.currentTarget.asInstanceOf[Element].getBoundingClientRect()
      contextMenu.openAt(
        () => toMenu(move, current, item, contextMenu, onChange),
        rect.left + window.scrollX,
        rect.bottom + window.scrollY
      )
    }

    L.span(
      L.cls(Styles.locations),
      toButton(s"Moved from ${current.from.label}", current.from.label, openMenu),
      L.span(L.cls(Styles.arrow), "→"),
      toButton(s"Moved to ${current.to.label}", current.to.label, openMenu)
    )
  }

  private def toButton(description: String, label: String, openMenu: Observer[Event]): L.Button =
    Button(_.handledWith(identity) --> openMenu).amend(
      L.cls(Styles.location),
      L.aria.label(s"$description. Change where it's moved"),
      L.aria.hasPopup(true),
      label
    )

  private def toMenu(
    move: Effect.MoveItem,
    current: ItemRoute,
    item: Item,
    contextMenu: ContextMenu,
    onChange: Observer[Effect.MoveItem]
  ): L.HtmlElement = {
    // A route the item can't take stays listed while it's the current one, so the menu still
    // shows where the move goes. Step validation reports it as a problem.
    val routes = ItemRoute.allFor(item)
    val options = if (routes.contains(current)) routes else current +: routes

    ContextMenuList.from(
      options.map { route =>
        val button = Button(_.handled --> { _ =>
          if (route != current) onChange.onNext(route.applyTo(move))
          contextMenu.close()
        })
        val icon = if (route == current) FontAwesome.icon(FreeSolid.faCheck) else L.span()
        ContextMenuList.Item(icon, route.label, button)
      }
    )
  }

  @js.native @JSImport("/styles/planning/details/moveLocations.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val locations: String = js.native
    val location: String = js.native
    val arrow: String = js.native
  }
}
