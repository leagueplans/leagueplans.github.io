package ddm.ui.component.plan.editing.effect

import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent, SelectComponent}
import ddm.ui.component.plan.editing.ItemSearchComponent
import ddm.ui.model.plan.Effect.GainItem
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.{Depository, Item}
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{CtorType, ScalaComponent}

object GainItemComponent {
  val build: Component[AddEffectComponent.Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[AddEffectComponent.Props]
      .render_P(render)
      .build

  private val countInput = NumberInputComponent.build[Int](1)
  private val depositoryTarget = SelectComponent.build[Depository.ID](Depository.inventory.id)

  private val withCountInput: With[Int] =
    render => countInput(NumberInputComponent.Props(
      id = "exp-entry",
      min = 0,
      max = Int.MaxValue,
      step = 1,
      render
    ))

  private def withTarget(player: Player): With[Depository.ID] =
    render => depositoryTarget(SelectComponent.Props(
      id = "target-depository",
      options = player.depositories.keys.map(id => id.raw -> id).toList,
      render
    ))

  private def withItemSearch(fuse: Fuse[Item]): With[Option[Item]] =
    render => ItemSearchComponent.build(ItemSearchComponent.Props(fuse, render))

  private def render(props: AddEffectComponent.Props): VdomNode =
    withCountInput((count, countInput) =>
      withTarget(props.player)((target, targetInput) =>
        withItemSearch(props.fuse) { (maybeItem, itemSearch) =>
          val maybeEffect = maybeItem.map(item => GainItem(item.id, count, target))

          FormComponent.build(FormComponent.Props(
            maybeEffect.map(props.onSubmit).getOrEmpty.when(count > 0).void,
            formContents = TagMod(countInput, targetInput, itemSearch)
          ))
        }
      )
    )
}
