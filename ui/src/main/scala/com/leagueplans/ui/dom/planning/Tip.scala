package com.leagueplans.ui.dom.planning

import com.leagueplans.uicommon.dom.Button
import com.leagueplans.uicommon.utils.laminar.EventProcessorOps.handledAs
import com.raquo.laminar.api.{L, nodeOptionToModifier, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** A hint for someone new to the planner, which looks the same wherever it shows: a title saying
  * what to do next, and a sentence or two saying how and why. Rather than offering a button of its
  * own, a tip makes the control it's talking about glow, so that the user learns where it is.
  * Where the tip sits is up to the caller.
  */
object Tip {
  /** @param hide the tips this one is among, which it offers to hide for good. Hints that are all
    *             the page has to show, such as in an empty plan, can't be hidden.
    */
  def apply(title: String, hide: Option[PlanningTips], content: L.Modifier[L.Div]*): L.Div =
    L.div(
      L.cls(Styles.tip),
      L.div(
        L.cls(Styles.head),
        L.span(L.cls(Styles.title), title),
        hide.map(hideButton)
      ),
      content
    )

  def text(content: L.Modifier[L.HtmlElement]*): L.HtmlElement =
    L.p(L.cls(Styles.text), content)

  /** A button for one of several choices, where there's more than one control the tip could
    * point at */
  def button(onClick: () => Unit, content: L.Modifier[L.Button]*): L.Button =
    Button(_.handledAs(()) --> (_ => onClick())).amend(L.cls(Styles.button), content)

  /** A remark beside a button, such as what it's for */
  def aside(content: L.Modifier[L.HtmlElement]*): L.HtmlElement =
    L.span(L.cls(Styles.aside), content)

  def key(name: String): L.HtmlElement =
    L.kbd(L.cls(Styles.key), name)

  private def hideButton(tips: PlanningTips): L.Button =
    Button(_.handledAs(()) --> (_ => tips.hide())).amend(L.cls(Styles.hide), "Hide tips")

  @js.native @JSImport("/styles/planning/tip.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val tip: String = js.native
    val head: String = js.native
    val title: String = js.native
    val text: String = js.native
    val button: String = js.native
    val aside: String = js.native
    val key: String = js.native
    val hide: String = js.native
  }
}
