package ddm.ui.component.plan.editing.effect

import cats.data.NonEmptyList
import ddm.ui.component.With
import ddm.ui.component.common.form.RadioButtonComponent
import ddm.ui.component.common.form.RadioButtonComponent.Choice
import ddm.ui.model.plan.Effect
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.feature.ReactFragment
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object AddEffectComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  type Submit = Effect => Callback

  final case class Props(
    itemCache: ItemCache,
    player: Player,
    onSubmit: Submit
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    // Here be dragons
    // I initially tried to write the below code without the map on the list (converting the
    // components to (Props => VdomNode)s), but that resulted in compilation errors stating
    // "UTF8 string too large".
    //
    // My best guess from a bit of digging is that some sort of implicit search is failing,
    // because there were some monstrously long types created from implicit resolution
    private val effectSelectComponent = new RadioButtonComponent[Props => VdomNode]
    private val effects: NonEmptyList[(String, Props => VdomNode)] =
    NonEmptyList.of(
      "Complete quest" -> CompleteQuestComponent.build,
      "Complete task" -> CompleteTaskComponent.build,
      "Drop item" -> DropItemComponent.build,
      "Move item" -> MoveItemComponent.build,
    ).map( { case (k, builder) => k -> { p: Props => builder(p) }})

    def render(props: Props): VdomNode =
      withEffectSelect((componentBuilder, effectSelect) =>
        <.div(
          ^.className := "add-effect",
          effectSelect,
          componentBuilder(props)
        )
      )

    private val withEffectSelect: With[Props => VdomNode] =
      render => effectSelectComponent(
        groupName = "effect-select",
        effects.map { case (label, effect) =>
          Choice(
            effect,
            id = label,
            label = label,
            radioTags = TagMod.empty,
            labelTags = <.span(label),
            render = ReactFragment(_, _)
          )
        },
        render
      )
  }
}
