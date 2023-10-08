package ddm.ui

import com.raquo.laminar.api.L
import ddm.common.model.Item
import ddm.ui.dom.Coordinator
import ddm.ui.model.common.forest.Forest
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Quest
import ddm.ui.model.player.item.ItemCache
import io.circe.scalajs.decodeJs
import io.circe.{Codec, Decoder}
import org.scalajs.dom.{document, window}

import java.util.UUID
import scala.annotation.nowarn
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object Main extends App {
  @js.native @JSImport("/data/items.json", JSImport.Default)
  private val itemsJson: js.Object = js.native

  @js.native @JSImport("/data/quests.json", JSImport.Default)
  private val questsJson: js.Object = js.native

  @js.native @JSImport("/data/plan.json", JSImport.Default)
  private val defaultPlanJson: js.Object = js.native

  private implicit val forestCodec: Codec[Forest[UUID, Step]] =
    Forest.codec(_.id)

  withResource[Set[Item]](itemsJson)(items =>
    withResource[List[Quest]](questsJson)(quests =>
      withResource[Forest[UUID, Step]](defaultPlanJson)(defaultPlan =>
        L.documentEvents.onDomContentLoaded.foreach { _ =>

          val container = document.createElement("div")
          document.body.appendChild(container)
          L.render(container, Coordinator(
            new StorageManager[Forest[UUID, Step]]("plan", window.localStorage),
            defaultPlan,
            ItemCache(items),
            quests
          ))
        }(L.unsafeWindowOwner): @nowarn("msg=discarded non-Unit value")
      )
    )
  )

  private def withResource[T : Decoder](obj: js.Object)(f: T => Unit): Unit =
    decodeJs[T](obj).foreach(f)
}
