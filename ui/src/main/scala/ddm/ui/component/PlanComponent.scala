package ddm.ui.component

import ddm.ui.model.plan.Step
import japgolly.scalajs.react.ScalaComponent
import japgolly.scalajs.react.component.Scala.{BackendScope, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._

object PlanComponent {
  final class Backend(scope: BackendScope[Unit, List[Step]]) {
    def render(s: List[Step]): VdomElement =
      <.div(
        ^.className := "plan",
        s.toTagMod(step =>
          StepComponent(step, StepComponent.Theme.Dark)
        )
      )
  }

  private val testSteps =
    Step(
      "Complete Cook's Assistant",
      List.empty,
      List(
        Step(
          "Start the quest",
          List((), ()),
          List(
            Step(
              "Talk to the Cook in Lumbridge Castle",
              List.empty,
              List.empty
            ),
            Step(
              "Grab the pot from the table",
              List.empty,
              List.empty
            ),
            Step(
              "Go into the basement",
              List.empty,
              List.empty
            ),
            Step(
              "Grab the bucket from by the sink",
              List.empty,
              List.empty
            )
          )
        ),
        Step(
          "Obtain the supplies",
          List(()),
          List(
            Step(
              "Obtain the egg",
              List.empty,
              List(
                Step(
                  "Go to the chicken pen north-west of Fred's farmhouse",
                  List.empty,
                  List.empty
                ),
                Step(
                  "Pick up an egg",
                  List.empty,
                  List.empty
                )
              )
            ),
            Step(
              "Obtain the flour",
              List.empty,
              List(
                Step(
                  "Go to the grain field west of the chicken pen",
                  List.empty,
                  List.empty
                ),
                Step(
                  "Pick some grain",
                  List.empty,
                  List.empty
                ),
                Step(
                  "Go to the mill and make the flour",
                  List.empty,
                  List(
                    Step(
                      "Climb to the second floor",
                      List.empty,
                      List.empty
                    ),
                    Step(
                      "Use the grain on the hopper",
                      List.empty,
                      List.empty
                    ),
                    Step(
                      "Pull the hopper controls",
                      List.empty,
                      List.empty
                    ),
                    Step(
                      "Descend to the ground floor",
                      List.empty,
                      List.empty
                    ),
                    Step(
                      "Use the pot on the flour bin",
                      List.empty,
                      List.empty
                    )
                  )
                ),
              )
            ),
            Step(
              "Obtain the milk",
              List.empty,
              List(
                Step(
                  "Go to the cow pen north of the mill",
                  List.empty,
                  List.empty
                ),
                Step(
                  "Use the bucket of the dairy cow",
                  List.empty,
                  List.empty
                )
              )
            )
          )
        ),
        Step(
          "Complete the quest",
          List.empty,
          List(
            Step(
              "Return to Lumbridge Castle",
              List.empty,
              List.empty
            ),
            Step(
              "Speak to the Cook",
              List.empty,
              List.empty
            )
          )
        )
      )
    )

  def apply(): Unmounted[Unit, List[Step], Backend] =
    ScalaComponent
      .builder[Unit]
      .initialState[List[Step]](List(testSteps))
      .renderBackend[Backend]
      .build
      .apply()
}
