package ddm.ui.model.plan

import java.util.UUID
import scala.annotation.tailrec

sealed trait Plan[T] {
  def steps(t: T): List[Step]
}

object Plan {
  def apply[T](implicit sl: Plan[T]): Plan[T] = sl

  implicit final class PlanOps[T : Plan](val t: T) {
    def takeUntil(lastID: UUID): List[Step] = {
      val (lhs, rhs) = flattenSteps.span(_.id != lastID)
      lhs ++ rhs.headOption
    }

    def flattenSteps: List[Step] =
      flattenHelper(acc = List.empty, remaining = Plan[T].steps(t))

    @tailrec
    private def flattenHelper(acc: List[Step], remaining: List[Step]): List[Step] =
      remaining match {
        case Nil => acc
        case h :: t => flattenHelper(acc = acc :+ h, remaining = h.substeps ++ t)
      }
  }

  implicit val stepInstance: Plan[Step] =
    new Plan[Step] {
      def steps(step: Step): List[Step] = List(step)
    }

  implicit val listStepInstance: Plan[List[Step]] =
    new Plan[List[Step]] {
      def steps(steps: List[Step]): List[Step] = steps
    }
}
