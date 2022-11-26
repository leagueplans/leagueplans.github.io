package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, SelectComponent}
import ddm.ui.component.plan.editing.effect.AddEffectComponent.Props
import ddm.ui.component.plan.editing.effect.SelectItemFromDepositoryComponent.SelectedItem
import ddm.ui.model.plan.Effect.MoveItem
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, ItemCache}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

object MoveItemComponent {
  val build: ScalaComponent[AddEffectComponent.Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .renderBackend[Backend]
      .build

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val selectItemFromDepositoryComponent = SelectItemFromDepositoryComponent.build
    private val targetInput = SelectComponent.build[Depository.Kind](Depository.Kind.Inventory)
    private val formComponent = FormComponent.build

    def render(props: AddEffectComponent.Props): VdomNode =
      withItemSelection(props.player, props.itemCache)((maybeItem, itemSelection) =>
        withTarget { (target, targetInput) =>
          val maybeEffect =
            maybeItem
              .map(selected => MoveItem(selected.item, selected.count, selected.source, target))
              .filter(_.count > 0)
              .filter(m => m.source != m.target)

          formComponent(FormComponent.Props(
            maybeEffect.map(props.onSubmit).getOrEmpty.void,
            formContents = TagMod(itemSelection, targetInput)
          ))
        }
      )

    private def withItemSelection(player: Player, itemCache: ItemCache): With[Option[SelectedItem]] =
      render => selectItemFromDepositoryComponent(SelectItemFromDepositoryComponent.Props(
        player,
        itemCache,
        render
      ))

    private val withTarget: With[Depository.Kind] =
      render => targetInput(SelectComponent.Props(
        id = "target-depository-select",
        label = "Choose where to move the item to:",
        options = Depository.Kind.kinds.toList.map(k => k -> k.name),
        render
      ))
  }
}
