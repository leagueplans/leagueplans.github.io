package com.leagueplans.ui.dom.planning.player.stats.form

import com.leagueplans.common.model.Skill
import com.leagueplans.common.model.Skill.*
import com.leagueplans.ui.dom.common.Tooltip
import com.leagueplans.ui.model.player.skill.{Exp, Level}
import com.raquo.airstream.core.Signal
import com.raquo.laminar.api.{L, textToTextNode}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.html.Paragraph

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

object ProgressBar {
  private type Params = (
    current: Level,
    next: Option[Level],
    remaining: Option[Exp],
    progress: Double
  )

  def apply(skillSignal: Signal[Skill], expSignal: Signal[Exp]): L.Div = {
    val paramsSignal = expSignal.map(convertToParams)
    L.div(
      L.cls(Styles.progress),
      L.div(
        L.cls(Styles.bar),
        createProgressIndicator(skillSignal, paramsSignal),
        L.p(
          L.cls(Styles.currentLevel),
          L.text <-- paramsSignal.map(_.current.toString)
        ),
        L.child.maybe <-- paramsSignal.map(_.next.map(level =>
          L.p(L.cls(Styles.nextLevel), level.toString)
        )),
        createTooltip(paramsSignal)
      ),
      createExpRemainingIndicator(paramsSignal),
    )
  }

  @js.native @JSImport("/styles/planning/player/stats/form/progressBar.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val progress: String = js.native
    val bar: String = js.native
    val indicator: String = js.native
    val currentLevel: String = js.native
    val nextLevel: String = js.native
    val expRemaining: String = js.native
  }
  
  private def convertToParams(exp: Exp): Params = {
    val level = Level.of(exp)
    val maybeNext = level.next
    
    val (remaining, progress) = maybeNext match {
      case Some(next) =>
        val remaining = next.bound - exp
        val progress = 1 - (remaining.raw.toDouble / (next.bound - level.bound).raw)
        (Some(remaining), progress)
        
      case None =>
        (None, 1.0)
    }
    
    (level, maybeNext, remaining, progress * 100)
  }
  
  private def createProgressIndicator(skillSignal: Signal[Skill], paramsSignal: Signal[Params]): L.Div =
    L.div(
      L.cls(Styles.indicator),
      L.backgroundColor <-- skillSignal.map(skillToColour),
      L.width <-- paramsSignal.map(params => L.style.percent(params.progress.toInt))
    )

  // Sourced from RuneLite
  // https://github.com/runelite/runelite/blob/8ef9321dd8a5640d7925abdf528fde291455cfe7/runelite-client/src/main/java/net/runelite/client/ui/SkillColor.java#L31
  private val skillToColour: Map[Skill, String] =
    Map(
      Agility -> L.style.rgb(58, 60, 137),
      Attack -> L.style.rgb(155, 32, 7),
      Construction -> L.style.rgb(130, 116, 95),
      Cooking -> L.style.rgb(112, 35, 134),
      Crafting -> L.style.rgb(151, 110, 77),
      Defence -> L.style.rgb(98, 119, 190),
      Farming -> L.style.rgb(101, 152, 63),
      Firemaking -> L.style.rgb(189, 120, 25),
      Fishing -> L.style.rgb(106, 132, 164),
      Fletching -> L.style.rgb(3, 141, 125),
      Herblore -> L.style.rgb(7, 133, 9),
      Hitpoints -> L.style.rgb(131, 126, 126),
      Hunter -> L.style.rgb(92, 89, 65),
      Magic -> L.style.rgb(50, 80, 193),
      Mining -> L.style.rgb(93, 143, 167),
      Prayer -> L.style.rgb(159, 147, 35),
      Ranged -> L.style.rgb(109, 144, 23),
      Runecraft -> L.style.rgb(170, 141, 26),
      Slayer -> L.style.rgb(100, 100, 100),
      Smithing -> L.style.rgb(108, 107, 82),
      Strength -> L.style.rgb(4, 149, 90),
      Thieving -> L.style.rgb(108, 52, 87),
      Woodcutting -> L.style.rgb(52, 140, 37)
    )

  private def createExpRemainingIndicator(paramsSignal: Signal[Params]): ReactiveHtmlElement[Paragraph] =
    L.p(
      L.cls(Styles.expRemaining),
      L.text <-- paramsSignal.map(params =>
        (params.remaining, params.next) match {
          case (Some(remaining), Some(next)) => s"$remaining xp until level $next"
          case _ => "Maxed!"
        }
      )
    )

  private def createTooltip(paramsSignal: Signal[Params]): L.Modifier[L.HtmlElement] =
    Tooltip(
      L.span(
        L.text <-- paramsSignal.map(params =>
          s"${String.format("%.2f", params.progress)}%"
        )
      )
    )
}
