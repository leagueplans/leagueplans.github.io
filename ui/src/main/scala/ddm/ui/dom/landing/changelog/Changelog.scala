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
        new Date(2024, 10, 15),
        "Prerelease Leagues V task list",
        List(
          "You may need to reimport your plan as a Leagues V plan in order to get proper game mode detection working" + 
            " for the new tasks."
        )
      ),
      item(
        new Date(2024, 10, 15),
        "RuneLite bank tags",
        List(
          "You can now click a button in the inventory to export the current inventory contents as a" +
            " RuneLite bank tag."
        )
      ),
      item(
        new Date(2024, 10, 13),
        "Preliminary support for Leagues V",
        List(
          "Leagues V plans can now be created. You can convert an existing plan to a leagues V plan by" +
            " downloading it, and importing it as a new plan.",
          "Once the task list has been added to the wiki, I'll try to port it over to this site. This" +
            " process took about 90 minutes for the last league.",
          "Assuming I have enough time before the league starts, the new tasks will be deduped against the" +
            " tasks for prior leagues. This means that, for example, you could prepare a plan now that" +
            " tries to complete the leagues IV task to cut a tree. Once the new tasks have been imported," +
            " you can then convert that plan to a leagues V plan and you'll receive a warning if the task" +
            " to a cut a tree has been removed."
        )
      ),
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
