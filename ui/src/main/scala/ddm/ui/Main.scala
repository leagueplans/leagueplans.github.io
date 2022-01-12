package ddm.ui

import ddm.ui.component.plan.StepComponent
import ddm.ui.component.player.StatusComponent
import ddm.ui.model.EffectResolver
import ddm.ui.model.plan.Plan
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Item
import io.circe.parser.parse
import japgolly.scalajs.react.vdom.TagOf
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.experimental.Fetch
import org.scalajs.dom.raw.HTMLElement
import org.scalajs.dom.{Event, document}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object Main extends App {
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit =
    withItemCache { itemCache =>
      // Creating a container, since react raises a warning if we render
      // directly into the document body.
      val container = document.createElement("div")
      html(itemCache).renderIntoDOM(container)
      document.body.appendChild(container)
    }

  private def withItemCache(f: Map[Item.ID, Item] => Unit): Unit =
    Fetch
      .fetch("data/items.json")
      .toFuture
      .flatMap(_.text().toFuture)
      .map(rawJson =>
        for {
          json <- parse(rawJson)
          items <- json.as[List[Item]]
        } yield items.map(i => i.id -> i).toMap
      )
      .map(_.toTry)
      .flatMap(Future.fromTry)
      .foreach(f)

  private def html(itemCache: Map[Item.ID, Item]): TagOf[HTMLElement] = {
    val plan = Plan.test

    <.table(
      <.tbody(
        <.tr(
          <.td(
            StepComponent(plan, StepComponent.Theme.Dark)
          ),
          <.td(
            StatusComponent(
              EffectResolver.resolve(Player.initial, plan.allEffects: _*),
              itemCache
            )
          )
        )
      )
    )
  }
}
