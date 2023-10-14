package ddm.ui.dom.editor

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToNode}
import ddm.ui.dom.common.form.{FuseSearch, RadioGroup, StylisedRadio}
import ddm.ui.model.player.Quest
import ddm.ui.wrappers.fusejs.Fuse

object QuestSearch {
  def apply(quests: Fuse[Quest]): (L.Input, L.Label, L.Modifier[L.Element], Signal[Option[Quest]]) = {
    val (search, searchLabel, options) = fuseSearch(quests)
    val (radios, selection) = radioGroup(options)
    (search, searchLabel, radios, selection)
  }

  private def fuseSearch(
    quests: Fuse[Quest],
  ): (L.Input, L.Label, Signal[List[Quest]]) = {
    val (search, label, options) =
      FuseSearch(
        quests,
        s"quests-fuse-search",
        maxResults = 10,
        defaultResults = quests.elements.sortBy(_.id).take(10)
      )

    (search.amend(L.placeholder("Cook's Assistant")), label.amend("Quest:"), options)
  }

  private def radioGroup(options: Signal[List[Quest]]): (L.Modifier[L.Element], Signal[Option[Quest]]) =
    RadioGroup[Quest](
      s"quest-radios",
      options.map(_.map(quest => RadioGroup.Opt(quest, quest.id.toString))),
      render = (quest, checked, radio, label) =>
        StylisedRadio(toQuestElement(quest), checked, radio, label)
    )

  private def toQuestElement(quest: Quest): L.Modifier[L.Label] =
    quest.name
}
