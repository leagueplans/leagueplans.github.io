package ddm.ui.model.plan

import ddm.ui.model.player.item.Item
import ddm.ui.model.player.skill.{Exp, Skill}

object Plan {
  val test: Step =
    Step(
      "Complete Cook's Assistant",
      List.empty,
      List(
        Step(
          "Start the quest",
          List(),
          List(
            Step(
              "Talk to the Cook in Lumbridge Castle",
              List.empty,
              List.empty
            ),
            Step(
              "Grab the pot from the table",
              List(Effect.GainItem(Item("Pot", stackable = false), count = 1, target = "Inventory")),
              List.empty
            ),
            Step(
              "Go into the basement",
              List.empty,
              List.empty
            ),
            Step(
              "Grab the bucket from by the sink",
              List(Effect.GainItem(Item("Bucket", stackable = false), count = 1, target = "Inventory")),
              List.empty
            )
          )
        ),
        Step(
          "Obtain the supplies",
          List(),
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
                  List(Effect.GainItem(Item("Egg", stackable = false), count = 1, target = "Inventory")),
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
                  List(Effect.GainItem(Item("Grain", stackable = false), count = 1, target = "Inventory")),
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
                      List(Effect.DestroyItem(Item("Grain", stackable = false), count = 1, source = "Inventory")),
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
                      List(
                        Effect.DestroyItem(Item("Pot", stackable = false), count = 1, source = "Inventory"),
                        Effect.GainItem(Item("Pot of flour", stackable = false), count = 1, target = "Inventory")
                      ),
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
                  List(
                    Effect.DestroyItem(Item("Bucket", stackable = false), count = 1, source = "Inventory"),
                    Effect.GainItem(Item("Bucket of milk", stackable = false), count = 1, target = "Inventory")
                  ),
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
              List(
                Effect.DestroyItem(Item("Egg", stackable = false), count = 1, source = "Inventory"),
                Effect.DestroyItem(Item("Bucket of milk", stackable = false), count = 1, source = "Inventory"),
                Effect.DestroyItem(Item("Pot of flour", stackable = false), count = 1, source = "Inventory"),
                Effect.GainExp(Skill.Cooking, Exp(300)),
                Effect.GainQuestPoints(1)
              ),
              List.empty
            )
          )
        )
      )
    )
}
