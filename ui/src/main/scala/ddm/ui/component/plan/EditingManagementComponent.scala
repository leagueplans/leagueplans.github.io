package ddm.ui.component.plan

import cats.data.NonEmptyList
import ddm.ui.component.common.{RadioButtonComponent, ToggleButtonComponent}
import ddm.ui.model.common.Tree
import ddm.ui.model.plan.Step
import ddm.ui.model.player.Player
import ddm.ui.model.player.item.ItemCache
import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, CtorType, ScalaComponent}

object EditingManagementComponent {
  val build: Component[Props, Unit, Unit, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .render_P(render)
      .build

  final case class Props(
    player: Player,
    itemCache: ItemCache,
    focusedStep: Option[(Tree[Step], Tree[Step] => Callback)],
    render: (EditingMode, VdomNode) => VdomNode
  )

  sealed trait EditingMode

  object EditingMode {
    case object Locked extends EditingMode
    case object ModifyHierarchy extends EditingMode
    case object ModifyOrder extends EditingMode
  }

  private val editingToggle = ToggleButtonComponent.build[Boolean]
  private val dragModeSelection = RadioButtonComponent.build[Boolean]

  private def render(props: Props): VdomNode =
    editingToggle(ToggleButtonComponent.Props(
      initialT = false,
      initialButtonStyle = <.span("Edit"),
      alternativeT = true,
      alternativeButtonStyle = <.span("Lock"),
      renderWithEditingToggle(props, _, _)
    ))

  private def renderWithEditingToggle(
    props: Props,
    editingEnabled: Boolean,
    editingToggle: VdomNode
  ): VdomNode =
    dragModeSelection(RadioButtonComponent.Props(
      name = "dragModeSelection",
      NonEmptyList.of("Order" -> true, "Heirarchy" -> false),
      (modifyOrder, editingModeSelect) => {
        val editingMode =
          if (editingEnabled && modifyOrder) EditingMode.ModifyOrder
          else if (editingEnabled) EditingMode.ModifyHierarchy
               else EditingMode.Locked

        props.render(
          editingMode,
          <.div(
            ^.className := "editing-tools",
            editingToggle,
            TagMod(
              editingModeSelect,
              props.focusedStep.map { case (step, editStep) =>
                StepEditorComponent.build(StepEditorComponent.Props(
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
