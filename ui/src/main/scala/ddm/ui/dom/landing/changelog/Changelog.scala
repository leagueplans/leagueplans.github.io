package ddm.ui.dom.landing.changelog

import com.raquo.laminar.api.L
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLOListElement

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

object Changelog {
  def apply(): ReactiveHtmlElement[HTMLOListElement] =
    L.ol(
      L.cls(Styles.changelog),
      item(
        new Date(2024, 10, 11),
        "Fixed an issue that could prevent plans from being loaded",
        List.empty
      ),
      item(
        new Date(2024, 9, 20),
        "Reworked the plan save system",
        List(
          "Note that this change invalidates all older plans. Contact @Granarder if you'd like to try" +
            " to recover old plans.",
          "Reduced the file size of plans by ~80%.",
          "Fixed a number of situations where saved plans could be corrupted."
        )
      )
    )

  @js.native @JSImport("/styles/landing/changelog/changelog.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val changelog: String = js.native
    val change: String = js.native
  }

  private def item(
    date: Date,
    title: String,
    summaries: List[String]
  ): L.LI =
    L.li(
      L.cls(Styles.change),
      Change(date, title, summaries)
    )
}
