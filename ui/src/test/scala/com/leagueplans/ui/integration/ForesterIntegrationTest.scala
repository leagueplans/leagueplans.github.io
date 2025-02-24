package com.leagueplans.ui.integration

import com.leagueplans.ui.dom.planning.forest.Forester
import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.{Plan, Step}
import com.leagueplans.ui.model.player.mode.Armageddon
import com.leagueplans.ui.storage.model.PlanMetadata
import com.leagueplans.ui.storage.opfs.PlanDirectory
import com.leagueplans.ui.utils.airstream.ObservableOps.flatMapConcat
import com.leagueplans.ui.wrappers.opfs.{FileSystemError, MockDirectoryHandle}
import com.raquo.airstream.ownership.{ManualOwner, Owner}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{Assertion, EitherValues, OptionValues}

import scala.annotation.nowarn
import scala.util.Using

final class ForesterIntegrationTest
  extends AnyFreeSpec
    with Matchers
    with EitherValues
    with OptionValues {

  private def test(f: Forester[Step.ID, Step] => Unit)(
    expectedToChildren: Map[Step, List[Step]],
    expectedRoots: List[Step]
  ): Assertion = {
    val forest = Forest.empty[Step.ID, Step]
    val forester = Forester(forest)
    val directory = PlanDirectory(new MockDirectoryHandle)
    val expectedForest = toForest(expectedToChildren, expectedRoots)

    Using(new ManualOwner)(owner =>
      setUpStreaming(forest, forester, directory)(using owner)
      f(forester)

      val actualForest = forester.signal.now()
      val persistedForest = readPlan(directory)(using owner)

      withClue("Checking nodes:") {
        actualForest.nodes shouldEqual expectedForest.nodes
        persistedForest.nodes shouldEqual expectedForest.nodes
      }
      withClue("Checking toParent:") {
        actualForest.toParent shouldEqual expectedForest.toParent
        persistedForest.toParent shouldEqual expectedForest.toParent
      }
      withClue("Checking toChildren:") {
        actualForest.toChildren shouldEqual expectedForest.toChildren
        persistedForest.toChildren shouldEqual expectedForest.toChildren
      }
      withClue("Checking roots:") {
        actualForest.roots shouldEqual expectedForest.roots
        persistedForest.roots shouldEqual expectedForest.roots
      }
    )(_.killSubscriptions()).get
  }

  private def setUpStreaming(
    forest: Forest[Step.ID, Step],
    forester: Forester[Step.ID, Step],
    directory: PlanDirectory[?]
  )(using Owner): Unit = {
    // Required to ensure that forest updates are processed
    forester.signal.foreach(_ => ())

    directory
      .create(PlanMetadata("test"), Plan("test", forest, Armageddon.settings))
      .foreach(_ => ())
    forester
      .updateStream
      .flatMapConcat(directory.applyUpdate)
      .foreach(_ => ()): @nowarn("msg=discarded non-Unit value")
  }

  private def readPlan(directory: PlanDirectory[?])(using Owner): Forest[Step.ID, Step] = {
    var eventualResult = Option.empty[Either[FileSystemError, Plan]]
    directory.readPlan().foreach(result => eventualResult = Some(result))
    eventualResult.value.value.steps
  }

  private def toForest(toChildren: Map[Step, List[Step]], roots: List[Step]): Forest[Step.ID, Step] =
    Forest.from(
      (toChildren.keySet ++ toChildren.values.flatten.toSet).map(step => step.id -> step).toMap,
      toChildren.map((parent, children) => parent.id -> children.map(_.id)),
      roots.map(_.id)
    )

  "ForesterIntegration" - {
    val step1 = Step("1")
    val step1Updated = step1.deepCopy(description = "1.1")
    val step2 = Step("2")
    val step3 = Step("3")
    val step4 = Step("4")
    val step5 = Step("5")

    "An initial empty plan" in test(_ => ())(
      expectedToChildren = Map.empty,
      expectedRoots = List.empty
    )

    "Adding steps" - {
      "New root steps" - {
        "A single step" in test(_.add(step1))(
          expectedToChildren = Map(step1 -> List.empty),
          expectedRoots = List(step1)
        )

        "Multiple steps" in test { forester =>
          forester.add(step1)
          forester.add(step2)
        }(
          expectedToChildren = Map(step1 -> List.empty, step2 -> List.empty),
          expectedRoots = List(step1, step2)
        )
      }

      "New substeps" - {
        "A single substep" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
        }(
          expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
          expectedRoots = List(step1)
        )

        "Multiple substeps" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step1.id)
        }(
          expectedToChildren = Map(
            step1 -> List(step2, step3),
            step2 -> List.empty,
            step3 -> List.empty
          ),
          expectedRoots = List(step1)
        )

        "Nested substeps" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step2.id)
        }(
          expectedToChildren = Map(
            step1 -> List(step2),
            step2 -> List(step3),
            step3 -> List.empty
          ),
          expectedRoots = List(step1)
        )

        "A non-existent parent" in test(_.add(step2, step1.id))(
          expectedToChildren = Map(step2 -> List.empty),
          expectedRoots = List(step2)
        )
      }

      "Duplicate steps" - {
        "An existing root step" - {
          "remains a root" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step1)
            }(
              expectedToChildren = Map(step1 -> List.empty),
              expectedRoots = List(step1)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step1)
            }(
              expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
              expectedRoots = List(step1)
            )
          }

          "remains a root in the same position" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step1)
          }(
            expectedToChildren = Map(step1 -> List.empty, step2 -> List.empty),
            expectedRoots = List(step1, step2)
          )

          "becomes a substep" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step2, step1.id)
            }(
              expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
              expectedRoots = List(step1)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3, step2.id)
              forester.add(step2, step1.id)
            }(
              expectedToChildren = Map(
                step1 -> List(step2),
                step2 -> List(step3),
                step3 -> List.empty,
              ),
              expectedRoots = List(step1)
            )
          }

          "linked to itself" in test { forester =>
            forester.add(step1)
            forester.add(step1, step1.id)
          }(
            expectedToChildren = Map(step1 -> List.empty),
            expectedRoots = List(step1)
          )

          "linked to a direct descendant" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step1, step2.id)
          }(
            expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
            expectedRoots = List(step1)
          )

          "linked to an indirect descendant" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step2.id)
            forester.add(step1, step3.id)
          }(
            expectedToChildren = Map(
              step1 -> List(step2),
              step2 -> List(step3),
              step3 -> List.empty
            ),
            expectedRoots = List(step1)
          )
        }

        "An existing substep" - {
          "remains a substep" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step2, step1.id)
            }(
              expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
              expectedRoots = List(step1)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step3, step2.id)
              forester.add(step2, step1.id)
            }(
              expectedToChildren = Map(
                step1 -> List(step2),
                step2 -> List(step3),
                step3 -> List.empty
              ),
              expectedRoots = List(step1)
            )
          }

          "remains a substep in the same position" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step1.id)
            forester.add(step2, step1.id)
          }(
            expectedToChildren = Map(
              step1 -> List(step2, step3),
              step2 -> List.empty,
              step3 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "remains a substep, but shifts parent" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3, step1.id)
              forester.add(step3, step2.id)
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List(step3),
                step3 -> List.empty,
              ),
              expectedRoots = List(step1, step2)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3, step1.id)
              forester.add(step4, step3.id)
              forester.add(step3, step2.id)
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List(step3),
                step3 -> List(step4),
                step4 -> List.empty
              ),
              expectedRoots = List(step1, step2)
            )
          }

          "linked to itself" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step2, step2.id)
          }(
            expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
            expectedRoots = List(step1)
          )

          "linked to a direct descendant" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step2.id)
            forester.add(step2, step3.id)
          }(
            expectedToChildren = Map(
              step1 -> List(step2),
              step2 -> List(step3),
              step3 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "linked to an indirect descendant" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step2.id)
            forester.add(step4, step3.id)
            forester.add(step2, step4.id)
          }(
            expectedToChildren = Map(
              step1 -> List(step2),
              step2 -> List(step3),
              step3 -> List(step4),
              step4 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "becomes a root" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step2)
            }(
              expectedToChildren = Map(step1 -> List.empty, step2 -> List.empty),
              expectedRoots = List(step1, step2)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step3, step2.id)
              forester.add(step2)
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List(step3),
                step3 -> List.empty
              ),
              expectedRoots = List(step1, step2)
            )
          }
        }
      }

      "Updated steps" - {
        "An existing root step" - {
          "remains a root" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step1Updated)
            }(
              expectedToChildren = Map(step1Updated -> List.empty),
              expectedRoots = List(step1Updated)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step1Updated)
            }(
              expectedToChildren = Map(step1Updated -> List(step2), step2 -> List.empty),
              expectedRoots = List(step1Updated)
            )
          }

          "remains a root in the same position" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step1Updated)
          }(
            expectedToChildren = Map(step1Updated -> List.empty, step2 -> List.empty),
            expectedRoots = List(step1Updated, step2)
          )

          "becomes a substep" - {
            "when the step has no children" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step1Updated, step2.id)
            }(
              expectedToChildren = Map(step1Updated -> List.empty, step2 -> List(step1Updated)),
              expectedRoots = List(step2)
            )

            "when the step has children" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3, step1.id)
              forester.add(step1Updated, step2.id)
            }(
              expectedToChildren = Map(
                step1Updated -> List(step3),
                step2 -> List(step1Updated),
                step3 -> List.empty
              ),
              expectedRoots = List(step2)
            )
          }

          "linked to itself" in test { forester =>
            forester.add(step1)
            forester.add(step1Updated, step1.id)
          }(
            expectedToChildren = Map(step1Updated -> List.empty),
            expectedRoots = List(step1Updated)
          )

          "linked to a direct descendant" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step1Updated, step2.id)
          }(
            expectedToChildren = Map(step1Updated -> List(step2), step2 -> List.empty),
            expectedRoots = List(step1Updated)
          )

          "linked to an indirect descendant" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step2.id)
            forester.add(step1Updated, step3.id)
          }(
            expectedToChildren = Map(
              step1Updated -> List(step2),
              step2 -> List(step3),
              step3 -> List.empty
            ),
            expectedRoots = List(step1Updated)
          )
        }

        "An existing substep" - {
          "remains a substep" - {
            "when the step has no children" in test { forester =>
              forester.add(step2)
              forester.add(step1, step2.id)
              forester.add(step1Updated, step2.id)
            }(
              expectedToChildren = Map(step1Updated -> List.empty, step2 -> List(step1Updated)),
              expectedRoots = List(step2)
            )

            "when the step has children" in test { forester =>
              forester.add(step2)
              forester.add(step1, step2.id)
              forester.add(step3, step1.id)
              forester.add(step1Updated, step2.id)
            }(
              expectedToChildren = Map(
                step1Updated -> List(step3),
                step2 -> List(step1Updated),
                step3 -> List.empty
              ),
              expectedRoots = List(step2)
            )
          }

          "remains a substep in the same position" in test { forester =>
            forester.add(step2)
            forester.add(step1, step2.id)
            forester.add(step3, step2.id)
            forester.add(step1Updated, step2.id)
          }(
            expectedToChildren = Map(
              step1Updated -> List.empty,
              step2 -> List(step1Updated, step3),
              step3 -> List.empty
            ),
            expectedRoots = List(step2)
          )

          "remains a substep, but shifts parent" - {
            "when the step has no children" in test { forester =>
              forester.add(step2)
              forester.add(step3)
              forester.add(step1, step2.id)
              forester.add(step1Updated, step3.id)
            }(
              expectedToChildren = Map(
                step1Updated -> List.empty,
                step2 -> List.empty,
                step3 -> List(step1Updated),
              ),
              expectedRoots = List(step2, step3)
            )

            "when the step has children" in test { forester =>
              forester.add(step2)
              forester.add(step3)
              forester.add(step1, step2.id)
              forester.add(step4, step1.id)
              forester.add(step1Updated, step3.id)
            }(
              expectedToChildren = Map(
                step1Updated -> List(step4),
                step2 -> List.empty,
                step3 -> List(step1Updated),
                step4 -> List.empty,
              ),
              expectedRoots = List(step2, step3)
            )
          }

          "linked to itself" in test { forester =>
            forester.add(step2)
            forester.add(step1, step2.id)
            forester.add(step1Updated, step1.id)
          }(
            expectedToChildren = Map(step1Updated -> List.empty, step2 -> List(step1Updated)),
            expectedRoots = List(step2)
          )

          "linked to a direct descendant" in test { forester =>
            forester.add(step2)
            forester.add(step1, step2.id)
            forester.add(step3, step1.id)
            forester.add(step1Updated, step3.id)
          }(
            expectedToChildren = Map(
              step1Updated -> List(step3),
              step2 -> List(step1Updated),
              step3 -> List.empty
            ),
            expectedRoots = List(step2)
          )

          "linked to an indirect descendant" in test { forester =>
            forester.add(step2)
            forester.add(step1, step2.id)
            forester.add(step3, step1.id)
            forester.add(step4, step3.id)
            forester.add(step1Updated, step4.id)
          }(
            expectedToChildren = Map(
              step1Updated -> List(step3),
              step2 -> List(step1Updated),
              step3 -> List(step4),
              step4 -> List.empty
            ),
            expectedRoots = List(step2)
          )

          "becomes a root" - {
            "when the step has no children" in test { forester =>
              forester.add(step2)
              forester.add(step1, step2.id)
              forester.add(step1Updated)
            }(
              expectedToChildren = Map(step1Updated -> List.empty, step2 -> List.empty),
              expectedRoots = List(step2, step1Updated)
            )

            "when the step has children" in test { forester =>
              forester.add(step2)
              forester.add(step1, step2.id)
              forester.add(step3, step1.id)
              forester.add(step1Updated)
            }(
              expectedToChildren = Map(
                step1Updated -> List(step3),
                step2 -> List.empty,
                step3 -> List.empty
              ),
              expectedRoots = List(step2, step1Updated)
            )
          }
        }
      }
    }

    "Moving steps" - {
      "Root steps" - {
        "to another step" - {
          "when the moved step has no children" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.move(step1.id, step2.id)
          }(
            expectedToChildren = Map(step1 -> List.empty, step2 -> List(step1)),
            expectedRoots = List(step2)
          )

          "when the moved step has children" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3, step1.id)
            forester.move(step1.id, step2.id)
          }(
            expectedToChildren = Map(
              step1 -> List(step3),
              step2 -> List(step1),
              step3 -> List.empty
            ),
            expectedRoots = List(step2)
          )

          "with children" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3, step2.id)
            forester.move(step1.id, step2.id)
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List(step3, step1),
              step3 -> List.empty
            ),
            expectedRoots = List(step2)
          )
        }

        "to itself" in test { forester =>
          forester.add(step1)
          forester.move(step1.id, step1.id)
        }(
          expectedToChildren = Map(step1 -> List.empty),
          expectedRoots = List(step1)
        )

        "to a direct descendant" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.move(step1.id, step2.id)
        }(
          expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
          expectedRoots = List(step1)
        )

        "to an indirect descendant" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step2.id)
          forester.move(step1.id, step3.id)
        }(
          expectedToChildren = Map(
            step1 -> List(step2),
            step2 -> List(step3),
            step3 -> List.empty
          ),
          expectedRoots = List(step1)
        )

        "promoted" in test { forester =>
          forester.add(step1)
          forester.promoteToRoot(step1.id)
        }(
          expectedToChildren = Map(step1 -> List.empty),
          expectedRoots = List(step1)
        )

        "promoted keeps the same position" in test { forester =>
          forester.add(step1)
          forester.add(step2)
          forester.promoteToRoot(step1.id)
        }(
          expectedToChildren = Map(step1 -> List.empty, step2 -> List.empty),
          expectedRoots = List(step1, step2)
        )
      }

      "Substeps" - {
        "to another step" - {
          "when the moved step has no children" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3, step1.id)
            forester.move(step3.id, step2.id)
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List(step3),
              step3 -> List.empty
            ),
            expectedRoots = List(step1, step2)
          )

          "when the moved step has children" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3, step1.id)
            forester.add(step4, step3.id)
            forester.move(step3.id, step2.id)
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List(step3),
              step3 -> List(step4),
              step4 -> List.empty,
            ),
            expectedRoots = List(step1, step2)
          )

          "with children" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3, step1.id)
            forester.add(step4, step2.id)
            forester.move(step3.id, step2.id)
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List(step4, step3),
              step3 -> List.empty,
              step4 -> List.empty
            ),
            expectedRoots = List(step1, step2)
          )
        }

        "to the same parent" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.move(step2.id, step1.id)
        }(
          expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
          expectedRoots = List(step1)
        )

        "to the same parent keeps the same position" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step1.id)
          forester.move(step2.id, step1.id)
        }(
          expectedToChildren = Map(
            step1 -> List(step2, step3),
            step2 -> List.empty,
            step3 -> List.empty
          ),
          expectedRoots = List(step1)
        )

        "to itself" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.move(step2.id, step2.id)
        }(
          expectedToChildren = Map(step1 -> List(step2), step2 -> List.empty),
          expectedRoots = List(step1)
        )

        "to a direct descendant" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step2.id)
          forester.move(step2.id, step3.id)
        }(
          expectedToChildren = Map(
            step1 -> List(step2),
            step2 -> List(step3),
            step3 -> List.empty
          ),
          expectedRoots = List(step1)
        )

        "to an indirect descendant" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step2.id)
          forester.add(step4, step3.id)
          forester.move(step2.id, step4.id)
        }(
          expectedToChildren = Map(
            step1 -> List(step2),
            step2 -> List(step3),
            step3 -> List(step4),
            step4 -> List.empty
          ),
          expectedRoots = List(step1)
        )

        "promoted" - {
          "when the promoted step has no children" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.promoteToRoot(step2.id)
          }(
            expectedToChildren = Map(step1 -> List.empty, step2 -> List.empty),
            expectedRoots = List(step1, step2)
          )

          "when the promoted step has children" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step2.id)
            forester.promoteToRoot(step2.id)
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List(step3),
              step3 -> List.empty
            ),
            expectedRoots = List(step1, step2)
          )
        }
      }
    }

    "Removing steps" - {
      "Non-existent steps" in test(_.remove(step1.id))(
        expectedToChildren = Map.empty,
        expectedRoots = List.empty
      )

      "Root steps" - {
        "without children" in test { forester =>
          forester.add(step1)
          forester.add(step2)
          forester.add(step3)
          forester.remove(step2.id)
        }(
          expectedToChildren = Map(step1 -> List.empty, step3 -> List.empty),
          expectedRoots = List(step1, step3)
        )

        "with a child" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3)
          forester.remove(step1.id)
        }(
          expectedToChildren = Map(step3 -> List.empty),
          expectedRoots = List(step3)
        )

        "with multiple children" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3)
          forester.add(step4, step1.id)
          forester.remove(step1.id)
        }(
          expectedToChildren = Map(step3 -> List.empty),
          expectedRoots = List(step3)
        )

        "with nested children" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3)
          forester.add(step4, step2.id)
          forester.remove(step1.id)
        }(
          expectedToChildren = Map(step3 -> List.empty),
          expectedRoots = List(step3)
        )
      }

      "Substeps" - {
        "without children" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step1.id)
          forester.remove(step2.id)
        }(
          expectedToChildren = Map(step1 -> List(step3), step3 -> List.empty),
          expectedRoots = List(step1)
        )

        "with a child" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step1.id)
          forester.add(step4, step2.id)
          forester.remove(step2.id)
        }(
          expectedToChildren = Map(step1 -> List(step3), step3 -> List.empty),
          expectedRoots = List(step1)
        )

        "with multiple children" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step1.id)
          forester.add(step4, step2.id)
          forester.add(step5, step2.id)
          forester.remove(step2.id)
        }(
          expectedToChildren = Map(step1 -> List(step3), step3 -> List.empty),
          expectedRoots = List(step1)
        )

        "with nested children" in test { forester =>
          forester.add(step1)
          forester.add(step2, step1.id)
          forester.add(step3, step1.id)
          forester.add(step4, step2.id)
          forester.add(step5, step4.id)
          forester.remove(step2.id)
        }(
          expectedToChildren = Map(step1 -> List(step3), step3 -> List.empty),
          expectedRoots = List(step1)
        )
      }
    }

    "Updating steps" - {
      "Providing the full step" - {
        "with new data" in test { forester =>
          forester.add(step1)
          forester.update(step1Updated)
        }(
          expectedToChildren = Map(step1Updated -> List.empty),
          expectedRoots = List(step1Updated)
        )

        "with the same data" in test { forester =>
          forester.add(step1)
          forester.update(step1)
        }(
          expectedToChildren = Map(step1 -> List.empty),
          expectedRoots = List(step1)
        )

        "with an entirely new step" in test(_.update(step1))(
          expectedToChildren = Map(step1 -> List.empty),
          expectedRoots = List(step1)
        )
      }

      "Providing an update function" - {
        "with new data" in test { forester =>
          forester.add(step1)
          forester.update(step1.id, _ => step1Updated)
        }(
          expectedToChildren = Map(step1Updated -> List.empty),
          expectedRoots = List(step1Updated)
        )

        "with the same data" in test { forester =>
          forester.add(step1)
          forester.update(step1.id, _ => step1)
        }(
          expectedToChildren = Map(step1 -> List.empty),
          expectedRoots = List(step1)
        )

        "for a non-existent step" in test(_.update(step1.id, _ => step1))(
          expectedToChildren = Map.empty,
          expectedRoots = List.empty
        )
      }
    }

    "Reordering steps" - {
      "Root steps" - {
        "Success cases" - {
          "a full reordering" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3)
            forester.reorder(List(step3.id, step1.id, step2.id))
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List.empty,
              step3 -> List.empty
            ),
            expectedRoots = List(step3, step1, step2)
          )

          "a partial reordering" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3)
            forester.reorder(List(step1.id, step3.id, step2.id))
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List.empty,
              step3 -> List.empty
            ),
            expectedRoots = List(step1, step3, step2)
          )

          "no reordering" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3)
            forester.reorder(List(step1.id, step2.id, step3.id))
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List.empty,
              step3 -> List.empty
            ),
            expectedRoots = List(step1, step2, step3)
          )
        }

        "Failure cases" - {
          "no steps are provided" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3)
            forester.reorder(List.empty)
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List.empty,
              step3 -> List.empty
            ),
            expectedRoots = List(step1, step2, step3)
          )

          "not all steps are provided" in test { forester =>
            forester.add(step1)
            forester.add(step2)
            forester.add(step3)
            forester.reorder(List(step3.id, step1.id))
          }(
            expectedToChildren = Map(
              step1 -> List.empty,
              step2 -> List.empty,
              step3 -> List.empty
            ),
            expectedRoots = List(step1, step2, step3)
          )

          "an unexpected step is provided" - {
            "that doesn't exist" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3)
              forester.reorder(List(step3.id, step2.id, step1.id, step4.id))
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List.empty,
                step3 -> List.empty
              ),
              expectedRoots = List(step1, step2, step3)
            )

            "that exists" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3)
              forester.add(step4, step3.id)
              forester.reorder(List(step3.id, step2.id, step1.id, step4.id))
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List.empty,
                step3 -> List(step4),
                step4 -> List.empty
              ),
              expectedRoots = List(step1, step2, step3)
            )
          }

          "a step is duplicated" - {
            "case with a missing step" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3)
              forester.reorder(List(step3.id, step2.id, step2.id))
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List.empty,
                step3 -> List.empty
              ),
              expectedRoots = List(step1, step2, step3)
            )

            "case with too many steps" in test { forester =>
              forester.add(step1)
              forester.add(step2)
              forester.add(step3)
              forester.reorder(List(step3.id, step2.id, step1.id, step2.id))
            }(
              expectedToChildren = Map(
                step1 -> List.empty,
                step2 -> List.empty,
                step3 -> List.empty
              ),
              expectedRoots = List(step1, step2, step3)
            )
          }
        }
      }

      "Substeps" - {
        "Success cases" - {
          "a full reordering" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step1.id)
            forester.add(step4, step1.id)
            forester.reorder(List(step3.id, step4.id, step2.id))
          }(
            expectedToChildren = Map(
              step1 -> List(step3, step4, step2),
              step2 -> List.empty,
              step3 -> List.empty,
              step4 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "a partial reordering" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step1.id)
            forester.add(step4, step1.id)
            forester.reorder(List(step2.id, step4.id, step3.id))
          }(
            expectedToChildren = Map(
              step1 -> List(step2, step4, step3),
              step2 -> List.empty,
              step3 -> List.empty,
              step4 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "no reordering" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step1.id)
            forester.add(step4, step1.id)
            forester.reorder(List(step2.id, step3.id, step4.id))
          }(
            expectedToChildren = Map(
              step1 -> List(step2, step3, step4),
              step2 -> List.empty,
              step3 -> List.empty,
              step4 -> List.empty
            ),
            expectedRoots = List(step1)
          )
        }

        "Failure cases" - {
          "no steps are provided" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step1.id)
            forester.add(step4, step1.id)
            forester.reorder(List.empty)
          }(
            expectedToChildren = Map(
              step1 -> List(step2, step3, step4),
              step2 -> List.empty,
              step3 -> List.empty,
              step4 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "not all steps are provided" in test { forester =>
            forester.add(step1)
            forester.add(step2, step1.id)
            forester.add(step3, step1.id)
            forester.add(step4, step1.id)
            forester.reorder(List(step4.id, step2.id))
          }(
            expectedToChildren = Map(
              step1 -> List(step2, step3, step4),
              step2 -> List.empty,
              step3 -> List.empty,
              step4 -> List.empty
            ),
            expectedRoots = List(step1)
          )

          "an unexpected step is provided" - {
            "that doesn't exist" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step3, step1.id)
              forester.add(step4, step1.id)
              forester.reorder(List(step4.id, step3.id, step2.id, step5.id))
            }(
              expectedToChildren = Map(
                step1 -> List(step2, step3, step4),
                step2 -> List.empty,
                step3 -> List.empty,
                step4 -> List.empty
              ),
              expectedRoots = List(step1)
            )

            "that exists" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step3, step1.id)
              forester.add(step4, step1.id)
              forester.add(step5, step4.id)
              forester.reorder(List(step4.id, step3.id, step2.id, step5.id))
            }(
              expectedToChildren = Map(
                step1 -> List(step2, step3, step4),
                step2 -> List.empty,
                step3 -> List.empty,
                step4 -> List(step5),
                step5 -> List.empty,
              ),
              expectedRoots = List(step1)
            )
          }

          "a step is duplicated" - {
            "case with a missing step" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step3, step1.id)
              forester.add(step4, step1.id)
              forester.reorder(List(step4.id, step3.id, step3.id))
            }(
              expectedToChildren = Map(
                step1 -> List(step2, step3, step4),
                step2 -> List.empty,
                step3 -> List.empty,
                step4 -> List.empty
              ),
              expectedRoots = List(step1)
            )

            "case with too many steps" in test { forester =>
              forester.add(step1)
              forester.add(step2, step1.id)
              forester.add(step3, step1.id)
              forester.add(step4, step1.id)
              forester.reorder(List(step4.id, step3.id, step2.id, step3.id))
            }(
              expectedToChildren = Map(
                step1 -> List(step2, step3, step4),
                step2 -> List.empty,
                step3 -> List.empty,
                step4 -> List.empty
              ),
              expectedRoots = List(step1)
            )
          }
        }
      }
    }
  }
}
