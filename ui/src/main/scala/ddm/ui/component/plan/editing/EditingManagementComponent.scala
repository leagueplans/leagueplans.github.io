package ddm.ui.component.plan.editing

import cats.data.NonEmptyList
import ddm.ui.component.RenderE
import ddm.ui.component.common.ToggleButtonComponent
import ddm.ui.component.common.form.RadioButtonComponent
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

object EditingManagementComponent {
  val build: ScalaComponent[Props, Unit, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .renderBackend[Backend]
      .build

  final case class Props(
    player: Player,
    itemCache: ItemCache,
    focusedStep: Option[(Tree[Step], Tree[Step] => Callback)],
    render: RenderE[EditingMode, VdomNode]
  )

  sealed trait EditingMode

  object EditingMode {
    case object Locked extends EditingMode
    case object ModifyHierarchy extends EditingMode
    case object ModifyOrder extends EditingMode
  }

  final class Backend(scope: BackendScope[Props, Unit]) {
    private val toggleButtonComponent = ToggleButtonComponent.build[Boolean]
    private val radioButtonComponent = RadioButtonComponent.build[Boolean]
    private val stepEditorComponent = StepEditorComponent.build

    def render(props: Props): VdomNode =
      toggleButtonComponent(ToggleButtonComponent.Props(
        initial = false,
        initialContent = <.span("Edit"),
        alternative = true,
        alternativeContent = <.span("Lock"),
        renderWithEditingToggle(props, _, _)
      ))

    private def renderWithEditingToggle(
      props: Props,
      editingEnabled: Boolean,
      editingToggle: TagMod
    ): VdomNode =
      radioButtonComponent(RadioButtonComponent.Props(
        name = "dragModeSelection",
        NonEmptyList.of("Order" -> true, "Heirarchy" -> false),
        (modifyOrder, editingModeSelect) => {
          val editingMode =
            if (editingEnabled && modifyOrder)
              EditingMode.ModifyOrder
            else if (editingEnabled)
              EditingMode.ModifyHierarchy
            else
              EditingMode.Locked

          props.render(
            editingMode,
            <.div(
              ^.className := "editing-tools",
              editingToggle,
              TagMod(
                editingModeSelect,
                props.focusedStep.map { case (step, editStep) =>
                  stepEditorComponent(StepEditorComponent.Props(
                    step,
                    editStep,
                    props.player,
                    props.itemCache
                  ))
                }
              ).when(editingMode != EditingMode.Locked)
            )
          )
        }
      ))
  }
}
