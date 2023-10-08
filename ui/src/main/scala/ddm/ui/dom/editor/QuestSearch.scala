package ddm.ui.dom.editor

import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringValueMapper, textToNode}
import ddm.ui.dom.common.form.{FuseSearch, RadioGroup}
import ddm.ui.model.player.Quest
import ddm.ui.wrappers.fusejs.Fuse

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object QuestSearch {
  def apply(quests: Fuse[Quest]): (L.Input, L.Label, L.Modifier[L.Element], Signal[Option[Quest]]) = {
    val (search, searchLabel, options) = fuseSearch(quests)
    val (radios, selection) = radioGroup(options)
    (search, searchLabel, radios, selection)
  }

  @js.native @JSImport("/styles/editor/questSearch.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val radio: String = js.native
    val alternative: String = js.native
    val selection: String = js.native
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

    (search.amend(L.placeholder("Cook's Assistant")), label.amend(L.span("Quest:")), options)
  }

  private def radioGroup(options: Signal[List[Quest]]): (L.Modifier[L.Element], Signal[Option[Quest]]) =
    RadioGroup[Quest](
      s"quest-radios",
      options.map(_.map(quest => RadioGroup.Opt(quest, quest.id.toString))),
      render = radio
    )

  private def radio(
    quest: Quest,
    checked: Signal[Boolean],
    radio: L.Input,
    label: L.Label
  ): L.Children =
    List(
      radio.amend(L.cls(Styles.radio)),
      label.amend(
        L.cls <-- checked.map {
          case true => Styles.selection
          case false => Styles.alternative
        },
        L.span(quest.name)
      )
    )
}
