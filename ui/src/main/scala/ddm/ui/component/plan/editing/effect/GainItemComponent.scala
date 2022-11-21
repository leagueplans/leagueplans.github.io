package ddm.ui.component.plan.editing.effect

import ddm.common.model.Item
import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent, SelectComponent}
import ddm.ui.component.plan.editing.ItemSearchComponent
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.model.plan.Effect.GainItem
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.Depository
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object GainItemComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val countInput = NumberInputComponent.build[Int](1)
    private val depositoryTarget = SelectComponent.build[Depository.ID](Depository.inventory.id)
    private val itemSearchComponent = ItemSearchComponent.build
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withCountInput((count, countInput) =>
        withTarget(props.player)((target, targetInput) =>
          withItemSearch(props.fuse) { (maybeItem, itemSearch) =>
            val maybeEffect = maybeItem.map(item => GainItem(item.id, count, target))

            formComponent(FormComponent.Props(
              maybeEffect.map(props.onSubmit).getOrEmpty.when(count > 0).void,
              formContents = TagMod(countInput, targetInput, itemSearch)
            ))
          }
        )
      )

    private val withCountInput: With[Int] =
      render => countInput(NumberInputComponent.Props(
        id = "item-count-entry",
        label = "Amount:",
        min = 0,
        max = Int.MaxValue,
        step = 1,
        render
      ))

    private def withTarget(player: Player): With[Depository.ID] =
      render => depositoryTarget(SelectComponent.Props(
        id = "target-depository-select",
        label = "Choose where to place the item:",
        options = player.depositories.keys.map(id => id -> id.raw).toList,
        render
      ))

    private def withItemSearch(fuse: Fuse[Item]): With[Option[Item]] =
      render => itemSearchComponent(ItemSearchComponent.Props(fuse, render))
  }
}
