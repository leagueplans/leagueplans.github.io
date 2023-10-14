package ddm.ui.dom.editor

import cats.data.NonEmptyList
import com.raquo.airstream.core.{EventStream, Signal}
import com.raquo.laminar.api.{L, textToNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import ddm.ui.dom.common.form.{Form, Select, TextInput}
import ddm.ui.model.plan.Effect
import ddm.ui.model.plan.Effect.{CompleteQuest, CompleteTask}
import ddm.ui.model.player.Quest
import ddm.ui.model.player.league.{Task, TaskTier}
import ddm.ui.wrappers.fusejs.Fuse
import org.scalajs.dom.html.Div

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object NewEffectForm {
  def apply(quests: Fuse[Quest]): (L.FormElement, EventStream[Option[Effect]]) = {
    val (emptyForm, submitButton, formSubmissions) = Form()
    val (typeSelector, typeLabel, typeSignal) = effectTypeSelector()
    val (questSearch, questSignal) = toQuestSearch(quests)
    val (taskInput, taskSignal) = taskEntry()

    val form = emptyForm.amend(
      L.cls(Styles.form),
      L.div(
        typeLabel.amend("Effect type:"),
        typeSelector.amend(L.cls(Styles.input)),
        submitButton.amend(L.cls(Styles.submit))
      ),
      L.child <-- typeSignal.splitOne(identity) {
        case (EffectType.Task, _, _) => taskInput
        case (EffectType.Quest, _, _) => questSearch
      }
    )

    (form, effectSubmissions(formSubmissions, typeSignal, questSignal, taskSignal))
  }

  @js.native @JSImport("/styles/editor/newEffectForm.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val form: String = js.native

    val input: String = js.native
    val submit: String = js.native
  }

  private sealed trait EffectType
  private object EffectType {
    case object Quest extends EffectType
    case object Task extends EffectType
  }

  private def effectTypeSelector(): (L.Select, L.Label, Signal[EffectType]) =
    Select[EffectType](
      id = "new-effect-type-selection",
      NonEmptyList.of(
        Select.Opt(EffectType.Task, "Complete a task"),
        Select.Opt(EffectType.Quest, "Complete a quest")
      )
    )

  private def toQuestSearch(quests: Fuse[Quest]): (ReactiveHtmlElement[Div], Signal[Option[Quest]]) = {
    val (search, searchLabel, radios, selection) = QuestSearch(quests)
    val div = L.div(
      searchLabel,
      search.amend(L.cls(Styles.input)),
      radios
    )
    (div, selection)
  }

  private def taskEntry(): (ReactiveHtmlElement[Div], Signal[Option[Task]]) = {
    val (tierInput, tierLabel, tierSignal) = Select[TaskTier](
      id = "new-task-tier-selection",
      NonEmptyList.of(
        Select.Opt(TaskTier.Easy, "Easy"),
        Select.Opt(TaskTier.Medium, "Medium"),
        Select.Opt(TaskTier.Hard, "Hard"),
        Select.Opt(TaskTier.Elite, "Elite"),
        Select.Opt(TaskTier.Master, "Master"),
      )
    )

    val (nameInput, nameLabel, nameSignal) = TextInput(
      TextInput.Type.Text,
      id = "new-task-name-entry",
      initial = ""
    )

    val div = L.div(
      L.p(
        tierLabel.amend("Tier:"),
        tierInput.amend(L.cls(Styles.input))
      ),
      L.p(
        nameLabel.amend("Name:"),
        nameInput.amend(L.cls(Styles.input))
      )
    )

    val taskSignal =
      nameSignal
        .combineWith(tierSignal)
        .map {
          case ("", _) => None
          case (name, tier) => Some(Task(tier, name))
        }

    (div, taskSignal)
  }

  private def effectSubmissions(
    formSubmissions: EventStream[Unit],
    effectTypeSignal: Signal[EffectType],
    questSignal: Signal[Option[Quest]],
    taskSignal: Signal[Option[Task]]
  ): EventStream[Option[Effect]] =
    formSubmissions.sample(
      effectTypeSignal.flatMap {
        case EffectType.Quest => questSignal.map(_.map(CompleteQuest))
        case EffectType.Task => taskSignal.map(_.map(CompleteTask))
      }
    )
}
