package com.leagueplans.ui.dom.player.quest

import com.leagueplans.ui.dom.common.ContextMenu
import com.leagueplans.ui.dom.common.form.FuseSearch
import com.leagueplans.ui.facades.fusejs.FuseOptions
import com.leagueplans.ui.model.plan.Effect.CompleteQuest
import com.leagueplans.ui.model.player.{Cache, Player, Quest}
import com.leagueplans.ui.wrappers.fusejs.Fuse
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringSeqValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.OList

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object QuestList {
  def apply(
    playerSignal: Signal[Player],
    cache: Cache,
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenuController: ContextMenu.Controller
  ): L.Div = {
    val completedQuestsSignal = playerSignal.map(_.completedQuests)
    val allQuests = cache.quests.values.toList
    val (filter, questsSignal) = questFilter(allQuests)

    L.div(
      L.cls(Styles.panel, PanelStyles.panel),
      L.headerTag(
        L.cls(Styles.header, PanelStyles.header),
        L.img(L.cls(Styles.icon), L.src(icon), L.alt("Quest point icon")),
        "Quest list"
      ),
      filter.amend(L.cls(Styles.filter)),
      L.div(
        L.cls(Styles.quests),
        category(
          "Quests",
          questsSignal.map(_.filter(_.points > 0)),
          completedQuestsSignal,
          effectObserverSignal,
          contextMenuController
        ),
        category(
          "Miniquests",
          questsSignal.map(_.filter(_.points == 0)),
          completedQuestsSignal,
          effectObserverSignal,
          contextMenuController
        )
      ),
      L.child <-- completedQuestsSignal.map { quests =>
        val points = quests.toList.map(cache.quests(_).points).sum
        L.p(L.cls(Styles.footer), s"Quest points: $points")
      }
    )
  }

  @js.native @JSImport("/images/quest-point-icon.png", JSImport.Default)
  private val icon: String = js.native

  @js.native @JSImport("/styles/player/quest/questList.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
    val filter: String = js.native
    val quests: String = js.native
    val footer: String = js.native

    val category: String = js.native
    val categoryHeader: String = js.native
    val quest: String = js.native

    val icon: String = js.native
  }

  @js.native @JSImport("/styles/shared/player/panel.module.css", JSImport.Default)
  private object PanelStyles extends js.Object {
    val panel: String = js.native
    val header: String = js.native
  }

  private def questFilter(quests: List[Quest]): (L.Input, Signal[List[Quest]]) = {
    val (search, _, signal) = FuseSearch(
      toFuse(quests),
      id = "quest-list-fuse-search",
      initial = "",
      maxResults = quests.size
    )

    (search.amend(L.placeholder("filter")), signal.map(_.getOrElse(quests)))
  }


  private def toFuse(quests: List[Quest]) =
    Fuse(
      quests,
      new FuseOptions {
        keys = js.defined(js.Array("name"))
        threshold = js.defined(0.3)
      }
    )

  private def category(
    name: String,
    questsSignal: Signal[List[Quest]],
    completedQuestsSignal: Signal[Set[Int]],
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenuController: ContextMenu.Controller
  ): List[L.Node] =
    List(
      L.h4(L.cls(Styles.categoryHeader, PanelStyles.header), name),
      list(questsSignal, completedQuestsSignal, effectObserverSignal, contextMenuController)
    )

  private def list(
    questsSignal: Signal[List[Quest]],
    completedQuestsSignal: Signal[Set[Int]],
    effectObserverSignal: Signal[Option[Observer[CompleteQuest]]],
    contextMenuController: ContextMenu.Controller
  ): ReactiveHtmlElement[OList] =
    L.ol(
      L.cls(Styles.category),
      L.children <--
        questsSignal
          .map(_.sorted(questOrdering))
          .split(_.name)((_, quest, _) =>
            L.li(
              L.cls(Styles.quest),
              QuestElement(
                quest,
                completedQuestsSignal.map(_.contains(quest.id)),
                effectObserverSignal,
                contextMenuController
              )
            )
          )
    )

  private val questOrdering: Ordering[Quest] =
    Ordering[String].on(quest =>
      if (quest.name.startsWith("A "))
        quest.name.stripPrefix("A ")
      else if (quest.name.startsWith("The "))
        quest.name.stripPrefix("The ")
      else
        quest.name
    )
}
