package com.leagueplans.ui.dom.planning.section.changelog

import com.raquo.laminar.api.L

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ChangelogIcon {
  /** The changelog's icon for the section rail, with a dot while there are changes the viewer hasn't
    * seen. The dot is placed against the rail's button, which this is shown within. Screen readers
    * hear of the changes from the button's name (Sections.scala).
    */
  def apply(src: String): L.Span =
    L.span(
      L.cls(Styles.icon),
      L.img(L.src(src), L.alt("")),
      L.child.maybe <-- ChangelogSeen.unread.map(Option.when(_)(L.span(L.cls(Styles.dot))))
    )

  @js.native @JSImport("/styles/planning/section/changelog/changelogIcon.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val icon: String = js.native
    val dot: String = js.native
  }
}
