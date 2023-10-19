package ddm.ui.dom.player.diary

import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, textToTextNode}
import ddm.ui.dom.common.ContextMenu
import ddm.ui.facades.fontawesome.freesolid.FreeSolid
import ddm.ui.model.plan.Effect.CompleteDiaryTask
import ddm.ui.model.player.diary.{DiaryRegion, DiaryTier}
import ddm.ui.model.player.{Cache, Player}
import ddm.ui.utils.laminar.LaminarOps.{RichEventProp, RichL}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object DiaryPanel {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteDiaryTask]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div = {
    val completeTasksSignal = playerSignal.map(_.completedDiaryTasks)

    val tabVar = Var[Tab](Tab.Summary)
    val regionVar = Var(Option.empty[DiaryRegion])
    val tierVar = Var(Option.empty[DiaryTier])

    val summaryTab = DiarySummaryTab(
      completeTasksSignal,
      cache,
      Observer.combine(
        tabVar.writer.contramap[DiaryRegion](_ => Tab.Details),
        regionVar.writer.contramapSome
      ),
      Observer.combine(
        tabVar.writer.contramap[Option[DiaryTier]](_ => Tab.Details),
        tierVar.writer
      )
    )

    val detailsTab = DiaryDetailsTab(
      completeTasksSignal,
      cache,
      effectObserverSignal,
      contextMenuController,
      regionVar,
      tierVar
    )

    L.div(
      L.cls(Styles.panel, PanelStyles.panel),
      L.div(
        L.cls(Styles.header),
        toTabToggle(tabVar),
        L.headerTag(
          L.cls(Styles.title, PanelStyles.header),
          L.img(L.cls(Styles.titleIcon), L.src(icon), L.alt("Quest point icon")),
          "Achievement diaries"
        )
      ),
      L.child <-- tabVar.signal.map {
        case Tab.Summary => summaryTab.amend(L.cls(Styles.tab))
        case Tab.Details => detailsTab.amend(L.cls(Styles.tab))
      }
    )
  }

  private sealed trait Tab

  private object Tab {
    case object Summary extends Tab
    case object Details extends Tab
  }

  @js.native @JSImport("/images/achievement-diary-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/player/diary/diaryPanel.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
    val tabToggle: String = js.native
    val titleIcon: String = js.native
    val title: String = js.native
    val tab: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def toTabToggle(tabVar: Var[Tab]): L.Button =
    L.button(
      L.cls(Styles.tabToggle),
      L.`type`("button"),
      L.child <-- tabVar.signal.map {
        case Tab.Summary => L.icon(FreeSolid.faBars)
        case Tab.Details => L.icon(FreeSolid.faHouse)
      },
      L.onClick.ifUnhandledF(
        _.map(_.preventDefault())
          .sample(tabVar)
          .map {
            case Tab.Summary => Tab.Details
            case Tab.Details => Tab.Summary
          }
      ) --> tabVar.writer
    )
}
