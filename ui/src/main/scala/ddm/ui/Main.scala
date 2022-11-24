package ddm.ui

import ddm.common.model.Item
import ddm.ui.component.MainComponent
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.item.ItemCache
import io.circe.Decoder
import io.circe.scalajs.decodeJs
import org.scalajs.dom.{Event, document, window}

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Main extends App {
  @js.native @JSImport("/data/items.json", JSImport.Default)
  private val itemsJson: js.Object = js.native

  @js.native @JSImport("/data/plan.json", JSImport.Default)
  private val defaultPlanJson: js.Object = js.native

  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit =
    withResource[Set[Item]](itemsJson) { items =>
      withResource[Tree[Step]](defaultPlanJson) { defaultPlan =>
        // Creating a container, since react raises a warning if we render
        // directly into the document body.
        val container = document.createElement("div")
        document.body.appendChild(container)

        MainComponent.build(MainComponent.Props(
          new StorageManager[Tree[Step]]("plan", window.localStorage),
          defaultPlan,
          ItemCache(items)
        )).renderIntoDOM(container): @nowarn("msg=discarded non-Unit value")
      }
    }

  private def withResource[T : Decoder](obj: js.Object)(f: T => Unit): Unit =
    decodeJs[T](obj).foreach(f)
}
