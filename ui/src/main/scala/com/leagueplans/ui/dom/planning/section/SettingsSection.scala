package com.leagueplans.ui.dom.planning.section

import com.leagueplans.ui.dom.plans.actions.{DeleteButton, DownloadButton, DuplicateButton}
import com.leagueplans.ui.dom.plans.{ModeSelect, PlanName}
import com.leagueplans.ui.model.plan.Plan
import com.leagueplans.ui.model.player.mode.{MainGame, Mode}
import com.leagueplans.uicommon.dom.{Button, ToastHub}
import com.raquo.airstream.core.Observer
import com.raquo.airstream.state.Var
import com.raquo.laminar.api.{L, StringSeqValueMapper, enrichSource, eventPropToProcessor, textToTextNode}

import scala.concurrent.duration.DurationInt
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/** What can be done to the open plan as a whole, rather than to its steps */
object SettingsSection {
  def apply(ctx: SectionContext): L.Div = {
    val plan = ctx.openPlan

    val nameDraft = Var(plan.name.now())
    val modeDraft = Var[Mode](MainGame)
    val currentMode = ctx.settings.map {
      case Plan.Settings.Deferred(mode) => Some(mode)
      case _: Plan.Settings.Explicit => None
    }
    val isModeChange = currentMode.combineWith(modeDraft.signal).map((current, draft) => !current.contains(draft))

    val (modeSelect, modeLabel) = ModeSelect("settings-mode", modeDraft)

    L.div(
      L.cls(Styles.section),
      // The name and mode can change from elsewhere, such as another tab
      plan.name --> nameDraft.writer,
      ctx.settings --> Observer[Plan.Settings] {
        case Plan.Settings.Deferred(mode) => modeDraft.set(mode)
        case _: Plan.Settings.Explicit => ()
      },
      L.div(
        L.cls(Styles.column),
        L.div(
          L.cls(Styles.card),
          L.h2(L.cls(Styles.cardTitle), "This plan"),
          L.form(
            L.cls(Styles.fields),
            L.label(L.cls(Styles.label), L.forId("settings-name"), "Name"),
            L.div(
              L.cls(Styles.inline),
              L.input(
                L.cls(Styles.field),
                L.idAttr("settings-name"),
                L.nameAttr("settings-name"),
                L.`type`("text"),
                L.autoComplete("off"),
                L.controlled(
                  L.value <-- nameDraft.signal,
                  L.onInput.mapToValue --> nameDraft.writer
                )
              ),
              Button.submit().amend(
                L.cls(Styles.button),
                L.disabled <-- nameDraft.signal.combineWith(plan.name).map(!PlanName.isChange(_, _)),
                "Rename"
              )
            ),
            L.onSubmit.preventDefault --> Observer[Any] { _ =>
              PlanName.clean(nameDraft.now()).filter(PlanName.isChange(_, plan.name.now())).foreach { name =>
                plan.rename(name)
                nameDraft.set(name)
              }
            }
          ),
          L.form(
            L.cls(Styles.fields),
            modeLabel.amend(L.cls(Styles.label), "Game mode"),
            L.div(
              L.cls(Styles.inline),
              modeSelect.amend(L.cls(Styles.field)),
              Button.submit().amend(
                L.cls(Styles.button),
                L.disabled <-- isModeChange.map(!_),
                "Change mode"
              )
            ),
            // Shown before the mode's changed, too, so that it's clear what the mode decides
            L.p(
              L.cls(Styles.text),
              "The game mode controls the default XP multipliers, starting items, stats, and available" +
                " progression features (e.g. league tasks). Changing the game mode does not change your" +
                " step definitions."
            ),
            // Changing back is as easy, so this doesn't ask first
            L.onSubmit.preventDefault --> Observer[Any] { _ =>
              val mode = modeDraft.now()
              plan.changeMode(mode)
              ctx.toasts.publish(ToastHub.Type.Success, 5.seconds, s"“${plan.name.now()}” now uses ${mode.name}")
            }
          )
        ),
        L.div(
          L.cls(Styles.card),
          L.h2(L.cls(Styles.cardTitle), "Copies"),
          L.p(
            L.cls(Styles.text),
            "Plans are saved locally to your browser, and will be lost if you clear your browser's storage." +
              " No data is saved remotely."
          ),
          L.p(
            L.cls(Styles.text),
            "Downloaded plan files can be imported into any browser, including a friend's, from the plan" +
              " list. To open the list, click the plan's name at the top of the page."
          ),
          L.div(
            L.cls(Styles.buttons),
            DownloadButton(plan.id, plan.name, ctx.planManager.storage, ctx.toasts).amend(L.cls(Styles.button)),
            DuplicateButton(plan.id, plan.name, ctx.planManager, ctx.toasts).amend(L.cls(Styles.button))
          ),
          L.p(
            L.cls(Styles.text),
            L.text <-- plan.name.map(name =>
              s"Make a copy creates a new plan, “${DuplicateButton.copyName(name)}”, and switches to it."
            )
          )
        ),
        L.div(
          L.cls(Styles.card),
          L.h2(L.cls(Styles.cardTitle), "Delete"),
          L.p(L.cls(Styles.text), L.text <-- plan.name.map(DeleteButton.warning)),
          L.div(
            L.cls(Styles.buttons),
            DeleteButton(plan.name, ctx.planManager, ctx.modal).amend(L.cls(Styles.button, Styles.danger))
          )
        )
      )
    )
  }

  @js.native @JSImport("/styles/planning/section/settingsSection.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val section: String = js.native
    val column: String = js.native
    val card: String = js.native
    val cardTitle: String = js.native
    val fields: String = js.native
    val label: String = js.native
    val inline: String = js.native
    val field: String = js.native
    val button: String = js.native
    val danger: String = js.native
    val buttons: String = js.native
    val text: String = js.native
  }
}
