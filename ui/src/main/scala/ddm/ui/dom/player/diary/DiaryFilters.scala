package ddm.ui.dom.player.diary

import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToNode}
import ddm.ui.dom.common.form.Select
import ddm.ui.dom.player.diary.DiaryDetailsTab.Progress
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTier}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryFilters {
  def apply(
    regionVar: Var[Option[DiaryRegion]],
    tierVar: Var[Option[DiaryTier]],
    progressVar: Var[Option[Progress]]
  ): L.Div = {
    L.div(
      L.cls(Styles.section),
      L.h4(L.cls(PanelStyles.header, Styles.header), "Filters"),
      toInput(
        id = "region",
        labelText = "Region:",
        DiaryRegion.all.map(region => (region, region.name)),
        regionVar
      ),
      toInput(
        id = "tier",
        labelText = "Tier:",
        DiaryTier.all.map(tier => (tier, tier.toString)),
        tierVar
      ),
      toInput(
        id = "progress",
        labelText = "Progress:",
        List(Progress.Incomplete -> "Incomplete", Progress.Complete -> "Complete"),
        progressVar
      ),
    )
  }

  @js.native @JSImport("/styles/player/diary/diaryFilters.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val header: String = js.native

    val filter: String = js.native
    val label: String = js.native
    val input: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val header: String = js.native
  }

  private def toInput[T](
    id: String,
    labelText: String,
    opts: List[(T, String)],
    `var`: Var[Option[T]]
  ): L.Div = {
    val (input, label) =
      Select(
        id = s"diary-$id-filter",
        Select.Opt(None, "Any") +: opts.map { case (t, name) =>
          Select.Opt(Some(t), name)
        },
        `var`
      )

    L.div(
      L.cls(Styles.filter),
      label.amend(L.cls(Styles.label), labelText),
      input.amend(L.cls(Styles.input))
    )
  }
}
