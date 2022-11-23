package ddm.ui

import ddm.common.model.Item
import ddm.ui.component.MainComponent
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.item.ItemCache
import io.circe.Decoder
import io.circe.parser.decode
import org.scalajs.dom.{Event, document, fetch, window}
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.annotation.nowarn
import scala.concurrent.Future

object Main extends App {
  document.addEventListener[Event]("DOMContentLoaded", _ => setupUI())

  private def setupUI(): Unit =
    withResource[Set[Item]](ResourcePaths.itemsJson) { items =>
      withResource[Tree[Step]](ResourcePaths.defaultPlanJson) { defaultPlan =>
        // Creating a container, since react raises a warning if we render
        // directly into the document body.
        val container = document.createElement("div")
        document.body.appendChild(container)

        MainComponent.build(MainComponent.Props(
          new StorageManager[Tree[Step]](ResourcePaths.planStorageKey, window.localStorage),
          defaultPlan,
          ItemCache(items)
        )).renderIntoDOM(container): @nowarn("msg=discarded non-Unit value")
      }
    }

  private def withResource[T : Decoder](path: String)(f: T => Unit): Unit =
    fetch(path)
      .toFuture
      .flatMap(_.text().toFuture)
      .map(decode[T])
      .map(_.toTry)
      .flatMap(Future.fromTry)
      .foreach(f)
}
