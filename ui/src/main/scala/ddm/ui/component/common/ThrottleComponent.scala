package ddm.ui.component.common

import japgolly.scalajs.react.component.Scala.Component
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

object ThrottleComponent {
  def build[T](initial: T): Component[Props[T], State[T], Backend[T], CtorType.Props] =
    ScalaComponent
      .builder[Props[T]]
      .initialState[State[T]](State(initial, timer = None))
      .renderBackend[Backend[T]]
      .componentDidUpdate(update =>
        updateTimer(update).when(
          update.currentProps.upstream != update.prevProps.upstream
        ).void
      )
      .build

  private def updateTimer[T](
    update: ComponentDidUpdate[Props[T], State[T], Backend[T], _]
  ): Callback =
    cancelTimer(update) >>
      update.modState(_.copy(timer = Some(setTimer(update))))

  private def cancelTimer[T](
    update: ComponentDidUpdate[Props[T], State[T], Backend[T], _]
  ): Callback =
    Callback(update.currentState.timer.foreach(timers.clearTimeout))

  private def setTimer[T](
    update: ComponentDidUpdate[Props[T], State[T], Backend[T], _]
  ): SetTimeoutHandle =
    timers.setTimeout(update.currentProps.delay)(
      update
        .modState(_.copy(downstream = update.currentProps.upstream))
        .runNow()
    )

  final case class Props[T](
    upstream: T,
    delay: FiniteDuration,
    render: T => VdomNode
  )

  final case class State[T](downstream: T, timer: Option[SetTimeoutHandle])

  final class Backend[T](scope: BackendScope[Props[T], State[T]]) {
    def render(props: Props[T], state: State[T]): VdomNode =
      props.render(state.downstream)
  }
}
