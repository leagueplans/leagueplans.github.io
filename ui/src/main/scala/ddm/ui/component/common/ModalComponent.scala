package ddm.ui.component.common

import japgolly.scalajs.react.callback.Callback
import japgolly.scalajs.react.facade.SyntheticEvent
import japgolly.scalajs.react.vdom.Attr
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, Ref, ScalaComponent}
import org.scalajs.dom.HTMLDialogElement

object ModalComponent {
  def build(initialState: State): ScalaComponent[Unit, State, Backend, CtorType.Nullary] =
    ScalaComponent
      .builder[Unit]
      .initialState(initialState)
      .renderBackend[Backend]
      .componentDidMount(cdm =>
        Callback.when(cdm.state != State.Hidden)(cdm.backend.showModal())
      )
      .componentDidUpdate(cdu =>
        cdu.currentState match {
          case State.Hidden => cdu.backend.close()
          case State.Visible(_) => cdu.backend.showModal()
        }
      )
      .componentWillUnmount(_.backend.close())
      .build

  final class Controller(componentRef: Ref.WithScalaComponent[Unit, State, Backend, CtorType.Nullary]) {
    private val stateSetter: Ref.Get[State => Unit] =
      componentRef.map(_.setState)

    def hide(): Callback =
      stateSetter.foreach(_.apply(State.Hidden))

    def show(contents: VdomNode): Callback =
      stateSetter.foreach(_.apply(State.Visible(contents)))
  }

  sealed trait State

  object State {
    final case class Visible(contents: VdomNode) extends State
    case object Hidden extends State
  }

  // Currently does not exist in scalajs-react
  private def dialog: HtmlTagOf[HTMLDialogElement] =
    HtmlTagOf[HTMLDialogElement]("dialog")

  // Currently does not exist in scalajs-react
  private val onCancel: Attr.Event[SyntheticEvent] =
    Attr.Event("onCancel")

  final class Backend(scope: BackendScope[Unit, State]) {
    private val ref = Ref[HTMLDialogElement]

    private[ModalComponent] def showModal(): Callback =
      ref.foreach(dialog => if (!dialog.open) dialog.showModal())

    private[ModalComponent] def close(): Callback =
      ref.foreach(dialog => if (dialog.open) dialog.close())

    def render(state: State): VdomNode = {
      state match {
        case State.Hidden =>
          EmptyVdom

        case State.Visible(contents) =>
          dialog.withRef(ref)(
            ^.className := "modal",
            ^.aria.modal := true,
            onCancel --> scope.setState(State.Hidden),
            contents
          )
      }
    }
  }
}
