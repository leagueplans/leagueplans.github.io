package ddm.ui

import com.raquo.airstream.state.{StrictSignal, Var}
import ddm.ui.PlanStorage.Result
import ddm.ui.model.plan.SavedState
import io.circe.Error
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.scalajs.dom.Storage

import scala.util.Try

object PlanStorage {
  enum Result {
    case Success(plan: SavedState.Named)
    case Failure(error: Error)
    case None
  }
}

final class PlanStorage(delegate: Storage) {
  private val planNamespace: String = "@plan"
  private val plansVar: Var[Set[String]] = Var(listPlans())

  def plansSignal: StrictSignal[Set[String]] =
    plansVar.signal

  private def listPlans(): Set[String] =
    (0 until delegate.length)
      .map(delegate.key)
      .collect { case s"${`planNamespace`}:$name" => name }
      .toSet

  def savePlan(namedPlan: SavedState.Named): Try[Unit] =
    Try(delegate.setItem(toKey(namedPlan.name), namedPlan.savedState.asJson.noSpaces)).map { _ =>
      plansVar.update(_ + namedPlan.name)
      ()
    }

  def loadPlan(name: String): Result =
    rawPlanData(name).map(decode[SavedState](_)) match {
      case Some(Right(plan)) => Result.Success(SavedState.Named(name, plan))
      case Some(Left(error)) => Result.Failure(error)
      case None => Result.None
    }

  def deletePlan(name: String): Unit = {
    delegate.removeItem(toKey(name))
    plansVar.update(_ - name)
  }

  def rawPlanData(name: String): Option[String] =
    Option(delegate.getItem(toKey(name)))

  private def toKey(planName: String): String =
    s"$planNamespace:$planName"
}
