package ddm.ui.component.player.item

import ddm.common.model.Item
import ddm.ui.component.With
import ddm.ui.component.common.form.{FormComponent, NumberInputComponent}
import ddm.ui.model.plan.Effect.GainItem
import ddm.ui.model.player.item.Depository
import ddm.ui.wrappers.fusejs.Fuse
import japgolly.scalajs.react.component.Scala.Unmounted
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object GainItemComponent {
  private val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  def apply(
    target: Depository.Kind,
    items: Fuse[Item],
    onSubmit: Option[GainItem] => Callback
  ): Unmounted[Props, Unit, Backend] =
    build(Props(target, items, onSubmit))

  final case class Props(
    target: Depository.Kind,
    items: Fuse[Item],
    onSubmit: Option[GainItem] => Callback
  )

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val quantityInput = NumberInputComponent.build[Int](initial = 1)
    private val formComponent = FormComponent.build

    def render(props: Props): VdomNode =
      withQuantityInput((quantity, quantityInput) =>
        withItemSearch(props.items, quantity) { (maybeItem, itemSearch) =>
          val maybeEffect =
            maybeItem
              .map(item => GainItem(item.id, quantity, props.target))
              .filter(_ => quantity > 0)

          formComponent(FormComponent.Props(
            props.onSubmit(maybeEffect).void,
            formContents = TagMod(quantityInput, itemSearch)
          ))
        }
      )

    private val withQuantityInput: With[Int] =
      render => quantityInput(NumberInputComponent.Props(
        id = "item-quantity-entry",
        label = "Quantity:",
        min = 0,
        max = Int.MaxValue,
        step = 1,
        render
      ))

    private def withItemSearch(items: Fuse[Item], quantity: Int): With[Option[Item]] =
      render => ItemSearchComponent(items, quantity, render)
  }
}
