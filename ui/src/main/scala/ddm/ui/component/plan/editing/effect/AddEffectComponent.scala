package ddm.ui.component.plan.editing.effect

import cats.data.NonEmptyList
import ddm.ui.component.With
import ddm.ui.component.common.RadioButtonComponent
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Item, ItemCache}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object AddEffectComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  type Submit = Effect => Callback

  final case class Props(
    fuse: Fuse[Item],
    itemCache: ItemCache,
    player: Player,
    onSubmit: Submit
  )

  // Here be dragons
  // I initially tried to write the below code without the map on the empty list (converting
  // the components to (Props => VdomNode)s), but that resulted in compilation errors stating
  // "UTF8 string too large".
  //
  // My best guess from a bit of digging is that some sort of implicit search is failing,
  // because there were some monstrously long types created from implicit resolution
  private val effectSelectComponent = RadioButtonComponent.build[Props => VdomNode]
  private val effects: NonEmptyList[(String, Props => VdomNode)] =
    NonEmptyList.of(
      "Complete quest" -> CompleteQuestComponent.build,
      "Drop item" -> DropItemComponent.build,
      "Gain item" -> GainItemComponent.build,
      "Gain XP" -> GainExpComponent.build,
    ).map( { case (k, builder) => k -> { p: Props => builder(p) }})

  private val withEffectSelect: With[Props => VdomNode] =
    render => effectSelectComponent(RadioButtonComponent.Props(
      name = "effect-select",
      effects,
      render
    ))

  private def render(props: Props): VdomNode =
    withEffectSelect((componentBuilder, effectSelect) =>
      <.div(
        ^.className := "add-effect",
        effectSelect,
        componentBuilder(props)
      )
    )
}
