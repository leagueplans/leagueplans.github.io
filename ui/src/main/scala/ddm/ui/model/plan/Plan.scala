package ddm.ui.model.plan

import ddm.ui.model.player.item.{Depository, Item}
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
              List(Effect.GainItem(Item.ID("1931"), count = 1, target = Depository.ID.Inventory)), // Pot
              List.empty
            ),
            Step(
              "Go into the basement",
              List.empty,
              List.empty
            ),
            Step(
              "Grab the bucket from by the sink",
              List(Effect.GainItem(Item.ID("1925"), count = 1, target = Depository.ID.Inventory)), // Bucket
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
                  List(Effect.GainItem(Item.ID("1944"), count = 1, target = Depository.ID.Inventory)), // Egg
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
                  List(Effect.GainItem(Item.ID("1947"), count = 1, target = Depository.ID.Inventory)), // Grain
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
                      List(Effect.DestroyItem(Item.ID("1947"), count = 1, source = Depository.ID.Inventory)), // Grain
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
                        Effect.DestroyItem(Item.ID("1931"), count = 1, source = Depository.ID.Inventory), // Pot
                        Effect.GainItem(Item.ID("1933"), count = 1, target = Depository.ID.Inventory) // Pot of flour
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
                    Effect.DestroyItem(Item.ID("1925"), count = 1, source = Depository.ID.Inventory), // Bucket
                    Effect.GainItem(Item.ID("1927"), count = 1, target = Depository.ID.Inventory) // Bucket of milk
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
                Effect.DestroyItem(Item.ID("1944"), count = 1, source = Depository.ID.Inventory), // Egg
                Effect.DestroyItem(Item.ID("1927"), count = 1, source = Depository.ID.Inventory), // Bucket of milk
                Effect.DestroyItem(Item.ID("1933"), count = 1, source = Depository.ID.Inventory), // Pot of flour
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
