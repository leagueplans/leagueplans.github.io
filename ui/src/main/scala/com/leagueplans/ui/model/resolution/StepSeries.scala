package com.leagueplans.ui.model.resolution

import com.leagueplans.ui.model.common.forest.Forest
import com.leagueplans.ui.model.plan.Step

import scala.annotation.tailrec

object StepSeries {
  private[resolution] type Iteration[ID, T] = (id: ID, value: T, reps: Int)

  /** Iterate through the steps, applying the function `f` and
    * accumulating the results. Depth-first traversal.
    */
  def foldLeft[Acc](forest: Forest[Step.ID, Step], acc: Acc)(
    f: (acc: Acc, step: Step, reps: Int) => Acc
  ): Acc = {
    val iterationForest = forest.map[Iteration[Step.ID, Step]]((id, step) => (id, step, step.repetitions))
    val roots = iterationForest.roots.flatMap(iterationForest.get)
    foldLeftHelper(iterationForest, acc, roots)(f)
  }

  private[resolution] def foldLeftHelper[T, ID, Acc](
    forest: Forest[ID, Iteration[ID, T]],
    acc: Acc
  )(f: (Acc, T, Int) => Acc): Acc =
    foldLeftHelper(forest, acc, forest.roots.flatMap(forest.get))(f)

  @tailrec
  private def foldLeftHelper[T, ID, Acc](
    forest: Forest[ID, Iteration[ID, T]],
    acc: Acc,
    iterations: List[Iteration[ID, T]]
  )(f: (Acc, T, Int) => Acc): Acc =
    iterations match {
      case Nil => acc
      case h :: t =>
        if (h.reps <= 0)
          foldLeftHelper(forest, acc, t)(f)
        else  {
          val children = forest.children(h.id)
          if (children.isEmpty)
            foldLeftHelper(forest, f(acc, h.value, h.reps), t)(f)
          else
            foldLeftHelper(
              forest,
              f(acc, h.value, 1),
              (children :+ (h.id, h.value, h.reps - 1)) ++ t
            )(f)
        }
    }
}
