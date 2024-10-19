package ddm.ui.dom.landing.changelog

import com.raquo.laminar.api.{L, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLParagraphElement

import scala.scalajs.js
import scala.scalajs.js.Date
import scala.scalajs.js.annotation.JSImport

object Change {
  def apply(
    date: Date,
    title: String,
    summaries: List[String]
  ): L.Div =
    L.div(
      L.p(
        L.cls(Styles.title),
        s"${date.toDateString()} - $title"
      ),
      summaries match {
        case Nil => L.emptyNode
        case summary :: Nil => toSummary(summary)
        case _ =>
          L.ol(
            L.cls(Styles.summaryList),
            summaries.map(summary => L.li(toSummary(summary)))
          )
      }
    )

  @js.native @JSImport("/styles/landing/changelog/change.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val title: String = js.native
    val summaryList: String = js.native
    val summary: String = js.native
  }

  private def toSummary(description: String): ReactiveHtmlElement[HTMLParagraphElement] =
    L.p(
      L.cls(Styles.summary),
      description
    )
}
