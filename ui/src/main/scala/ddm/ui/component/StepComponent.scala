package ddm.ui.component

import ddm.ui.model.plan.Step
import japgolly.scalajs.react.component.Scala.{BackendScope, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{Callback, ScalaComponent}

object StepComponent {
  sealed trait Theme {
    val other: Theme
    val cssClass: String
  }

  object Theme {
    case object Light extends Theme {
      val other: Dark.type = Dark
      val cssClass: String = "light"
    }

    case object Dark extends Theme {
      val other: Light.type = Light
      val cssClass: String = "dark"
    }
  }

  final case class Props(step: Step, theme: Theme)

  final class Backend(scope: BackendScope[Props, Visibility]) {
    def render(p: Props, visibility: Visibility): VdomElement =
      <.div(
        ^.className := s"step-box row ${p.theme.cssClass}",
        StepVisibilityComponent(visibility, toggleVisibility),
        <.div(
          ^.className := "step-content",
          <.p(p.step.description),
          <.div(
            ^.classSet(visibility.cssClassSetter),
            p.step.effects.toTagMod(_ =>
              <.p("effect")
            ),
            p.step.substeps.toTagMod(substep =>
              StepComponent(substep, p.theme.other)
            )
          )
        )
      )

    def toggleVisibility: Callback =
      scope.modState(_.other)
  }

  def apply(step: Step, theme: Theme): Unmounted[Props, Visibility, Backend] =
    ScalaComponent
      .builder[Props]
      .initialState[Visibility](Visibility.Visible)
      .renderBackend[Backend]
      .build
      .apply(Props(step, theme))
}
