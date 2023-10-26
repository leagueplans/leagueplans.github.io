package ddm.ui.dom.player.task

import com.raquo.laminar.api.{L, seqToModifier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object TaskSummaryTab {
  def apply(categories: List[L.HtmlElement]): L.Div =
    L.div(
      L.cls(Styles.diaryOptions),
      categories.map(_.amend(L.cls(Styles.option)))
    )

  @js.native @JSImport("/styles/player/task/taskSummaryTab.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val diaryOptions: String = js.native
    val option: String = js.native
  }
}
