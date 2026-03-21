package com.leagueplans.ui.projection.calculation

import com.leagueplans.ui.model.common.forest.Forest
import org.scalajs.dom
import org.scalajs.macrotaskexecutor.MacrotaskExecutor.Implicits.global

import scala.concurrent.Future

private[calculation] object ForestFolder {
  type Iteration[ID, T] = (id: ID, value: T, reps: Int)

  def foldLeftAsync[T, ID, Acc](
    forest: Forest[ID, T],
    initialAcc: Acc,
    signal: dom.AbortSignal,
    reps: T => Int,
    yieldInterval: Int = 500
  )(f: (Acc, T, Int) => Acc): Future[Option[Acc]] = {
    val iterationForest = forest.map[Iteration[ID, T]]((id, value) => (id, value, reps(value)))
    val roots = iterationForest.roots.flatMap(iterationForest.get)
    foldLeftAsync(iterationForest, initialAcc, roots, signal, yieldInterval)(f)
  }

  def foldLeftAsync[T, ID, Acc](
    forest: Forest[ID, Iteration[ID, T]],
    initialAcc: Acc,
    initialRemaining: List[Iteration[ID, T]],
    signal: dom.AbortSignal,
    yieldInterval: Int
  )(f: (Acc, T, Int) => Acc): Future[Option[Acc]] = {
    var acc = initialAcc
    var remaining = initialRemaining
    var repsUntilYield = yieldInterval

    while (remaining.nonEmpty && repsUntilYield > 0 && !signal.aborted) {
      remaining match {
        case Nil => // unreachable; while condition ensures nonEmpty
        case h :: t =>
          if (h.reps <= 0) {
            remaining = t
          } else {
            val children = forest.children(h.id)
            if (children.isEmpty) {
              val excessReps = h.reps - repsUntilYield
              if (excessReps > 0) {
                acc = f(acc, h.value, repsUntilYield)
                remaining = (h.id, h.value, excessReps) +: t
                repsUntilYield = 0
              } else {
                acc = f(acc, h.value, h.reps)
                remaining = t
                repsUntilYield = -excessReps
              }
            } else {
              acc = f(acc, h.value, 1)
              remaining = (children :+ (h.id, h.value, h.reps - 1)) ++ t
              repsUntilYield -= 1
            }
          }
      }
    }

    if (signal.aborted) Future.successful(None)
    else if (remaining.isEmpty) Future.successful(Some(acc))
    else
      Future.unit.flatMap(_ =>
        foldLeftAsync(forest, acc, remaining, signal, yieldInterval)(f)
      )
  }
}
