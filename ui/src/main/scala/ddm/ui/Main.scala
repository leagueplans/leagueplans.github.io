package ddm.ui

import com.raquo.laminar.api.L
import ddm.common.model.Item
import ddm.ui.dom.Coordinator
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.item.ItemCache
import io.circe.Decoder
import io.circe.scalajs.decodeJs
import org.scalajs.dom.{document, window}

import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Main extends App {
  @js.native @JSImport("/data/items.json", JSImport.Default)
  private val itemsJson: js.Object = js.native

  @js.native @JSImport("/data/plan.json", JSImport.Default)
  private val defaultPlanJson: js.Object = js.native

  withResource[Set[Item]](itemsJson)(items =>
    withResource[Tree[Step]](defaultPlanJson)(defaultPlan =>
      L.documentEvents.onDomContentLoaded.foreach { _ =>
        val container = document.createElement("div")
        document.body.appendChild(container)
        L.render(container, Coordinator(
          new StorageManager[Tree[Step]]("plan", window.localStorage),
          defaultPlan,
          ItemCache(items)
        ))
      }(L.unsafeWindowOwner): @nowarn("msg=discarded non-Unit value")
    )
  )

  private def withResource[T : Decoder](obj: js.Object)(f: T => Unit): Unit =
    decodeJs[T](obj).foreach(f)
}
