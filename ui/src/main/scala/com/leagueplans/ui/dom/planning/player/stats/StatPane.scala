package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.plan.Effect
import com.leagueplans.ui.model.plan.Effect.{GainExp, UnlockSkill}
import com.leagueplans.ui.model.player.skill.Stat
import com.leagueplans.ui.utils.airstream.ObserverOps.observer
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.{Observer, Signal}
import com.raquo.laminar.api.{L, StringBooleanSeqValueMapper, seqToModifier, textToTextNode}
import com.raquo.laminar.modifiers.Binder

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatPane {
  def apply(
    stat: Signal[Stat],
    effectObserver: Signal[Option[Observer[Effect]]],
    contextMenuController: ContextMenu.Controller,
    modal: Modal
  ): L.Div = {
    val pane = toPane(stat).amend(toTooltip(stat))

    val gainXPFormOpener =
      stat
        .map(_.skill)
        .combineWith(effectObserver)
        .map((skill, maybeObserver) =>
          FormOpener(
            modal,
            GainXPForm(skill),
            maybeObserver.observer.contracollect[Option[GainExp]] { case Some(effect) => effect }
          )
        )

    val menuBinder = toMenuBinder(
      contextMenuController,
      stat,
      effectObserver,
      gainXPFormOpener
    )

    pane.amend(menuBinder)
  }

  @js.native @JSImport("/images/stat-window/stat-background.png", JSImport.Default)
  private val statBackground: String = js.native

  @js.native @JSImport("/styles/planning/player/stats/pane.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val pane: String = js.native
    val icon: String = js.native
    val locked: String = js.native
    val background: String = js.native
    val numerator: String = js.native
    val denominator: String = js.native
    val xp: String = js.native
  }

  private def toPane(stat: Signal[Stat]): L.Div =
    L.div(
      L.cls(Styles.pane),
      L.children <-- stat.splitOne(_.level)((level, _, _) =>
        List(
          L.span(L.cls(Styles.numerator), level.raw),
          L.span(L.cls(Styles.denominator), level.raw)
        )
      ),
      L.children <-- stat.splitOne(_.skill)((skill, _, _) =>
        List(
          SkillIcon(skill).amend(
            L.cls(Styles.icon),
            L.cls <-- stat.map(s => List(Styles.locked -> !s.unlocked))
          ),
          L.img(
            L.cls(Styles.background),
            L.src(statBackground),
            L.alt <-- stat.map(s => s"${s.skill} level")
          )
        )
      )
    )

  private def toTooltip(statSignal: Signal[Stat]): L.Modifier[L.HtmlElement] = {
    val xpRow = dynamicSpan(statSignal)(s => s"${s.skill} XP:") -> xpValue(dynamicSpan(statSignal)(_.exp.toString))

    val rows =
      statSignal
        .map(_.level.next)
        .split(_ => ()) { case ((), _, nextLevelSignal) =>
          val remainingXP =
            nextLevelSignal
              .withCurrentValueOf(statSignal)
              .map((next, stat) => next.bound - stat.exp)

          List(
            L.span("Next level at:") -> xpValue(dynamicSpan(nextLevelSignal)(_.bound.toString)),
            L.span("Remaining XP:") -> xpValue(dynamicSpan(remainingXP)(_.toString))
          )
        }
        .map(optionalRows => xpRow +: optionalRows.toList.flatten)


    Tooltip(KeyValuePairs(rows))
  }

  private def dynamicSpan[T](signal: Signal[T])(f: T => String): L.Span =
    L.span(L.child.text <-- signal.map(f))

  private def xpValue(span: L.Span): L.Modifier[L.HtmlElement] =
    List(L.cls(Styles.xp), span)

  private def toMenuBinder(
    controller: ContextMenu.Controller,
    statSignal: Signal[Stat],
    effectObserverSignal: Signal[Option[Observer[Effect]]],
    gainXPFormOpenerSignal: Signal[FormOpener]
  ): Binder[L.Element] =
    controller.bind(closer =>
      Signal
        .combine(statSignal, effectObserverSignal, gainXPFormOpenerSignal)
        .map((stat, maybeEffectObserver, gainXPFormOpener) =>
          maybeEffectObserver.map(observer =>
            toMenu(stat, gainXPFormOpener, observer, closer)
          )
        )
    )

  private def toMenu(
    stat: Stat,
    gainXPFormOpener: FormOpener,
    effectObserver: Observer[UnlockSkill],
    menuCloser: Observer[ContextMenu.CloseCommand]
  ): L.Button =
    if (stat.unlocked)
      Button(_.handled --> Observer.combine(menuCloser, gainXPFormOpener.toObserver)).amend("Gain XP")
    else
      Button(
        _.handled --> Observer.combine(
          menuCloser,
          effectObserver.contramap[Any](_ => UnlockSkill(stat.skill))
        )
      ).amend("Unlock skill")
}
