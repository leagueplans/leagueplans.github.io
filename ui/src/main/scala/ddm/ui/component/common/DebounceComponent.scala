package ddm.ui.component.common

import ddm.ui.component.RenderS
import ddm.ui.component.common.DebounceComponent.{Backend, Props, State}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.component.builder.Lifecycle.ComponentDidUpdate
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, Callback, CtorType, ScalaComponent}

import scala.concurrent.duration.FiniteDuration
import scala.scalajs.js.timers
import scala.scalajs.js.timers.SetTimeoutHandle

final class DebounceComponent[Up, Down] {
  private val build: Component[Props[Up, Down], State[Down], Backend[Up, Down], CtorType.Props] =
    ScalaComponent
      .builder[Props[Up, Down]]
      .initialStateFromProps(props => State(props.expensiveComputation(props.upstream), timer = None))
      .renderBackend[Backend[Up, Down]]
      .shouldComponentUpdatePure(update =>
        update.cmpProps(_ != _) || update.cmpState(_.downstream != _.downstream)
      )
      .componentDidUpdate(update =>
        Callback.when(update.prevProps.upstream != update.currentProps.upstream)(
          scheduleUpdate(update)
        ).void
      )
      .componentWillUnmount(update => cancelTimer(update.state))
      .build

  def apply(
    upstream: Up,
    delay: FiniteDuration,
    expensiveComputation: Up => Down,
    render: RenderS[Down]
  ): Unmounted[Props[Up, Down], State[Down], Backend[Up, Down]] =
    build.apply(Props(upstream, delay, expensiveComputation, render))

  private def scheduleUpdate(
    update: ComponentDidUpdate[Props[Up, Down], State[Down], Backend[Up, Down], Unit],
  ): Callback =
    cancelTimer(update.prevState) >>
      update.modState(_.copy(timer = Some(startTimer(update))))

  private def cancelTimer(state: State[Down]): Callback =
    Callback(state.timer.foreach(timers.clearTimeout))

  private def startTimer(
    update: ComponentDidUpdate[Props[Up, Down], State[Down], Backend[Up, Down], Unit]
  ): SetTimeoutHandle =
    timers.setTimeout(update.currentProps.delay)(
      update.modState(_.copy(
        downstream = update.currentProps.expensiveComputation(update.currentProps.upstream),
        timer = None
      )).runNow()
    )
}

object DebounceComponent {
  final case class Props[Up, Down](
    upstream: Up,
    delay: FiniteDuration,
    expensiveComputation: Up => Down,
    render: RenderS[Down]
  )

  final case class State[Down](downstream: Down, timer: Option[SetTimeoutHandle])

  final class Backend[Up, Down](scope: BackendScope[Props[Up, Down], State[Down]]) {
    def render(props: Props[Up, Down], state: State[Down]): VdomNode =
      props.render(state.downstream)
  }
}
