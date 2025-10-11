package com.leagueplans.ui.dom.planning.player.stats

import com.leagueplans.ui.dom.common.*
import com.leagueplans.ui.model.player.skill.Stat
import com.leagueplans.ui.utils.laminar.EventProcessorOps.handled
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, StringBooleanSeqValueMapper, seqToModifier, textToTextNode}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object StatPane {
  def apply(
    stat: Signal[Stat],
    showStatsDetailForm: () => Unit,
    formEnabled: Signal[Boolean]
  ): L.Button =
    Button(_.handled --> (_ => showStatsDetailForm())).amend(
      L.cls(Styles.pane),
      L.disabled <-- formEnabled.invert,
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
          )
        )
      ),
      toTooltip(stat)
    )

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
}
