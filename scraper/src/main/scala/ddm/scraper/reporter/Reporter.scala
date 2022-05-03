package ddm.scraper.reporter

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.stream.scaladsl.Flow
import ddm.scraper.wiki.model.Page

import scala.util.Try

object Reporter {
  sealed trait Message

  object Message {
    final case class Failure(page: Page, cause: Throwable) extends Message
    final case class Publish(runStatus: Try[_]) extends Message
  }

  def init(baseURL: String, print: String => Unit): Behavior[Message] =
    behavior(baseURL, print, failures = List.empty)

  private def behavior(
    baseURL: String,
    print: String => Unit,
    failures: List[Message.Failure]
  ): Behavior[Message] =
    Behaviors.receiveMessage {
      case f: Message.Failure =>
        behavior(baseURL, print, failures :+ f)

      case Message.Publish(runStatus) =>
        print(ReportPrinter.print(runStatus, failures, baseURL))
        Behaviors.stopped
    }

  def flow[T](reporter: ActorRef[Message.Failure]): Flow[Either[(Page, Throwable), T], T, _] =
    Flow[Either[(Page, Throwable), T]].mapConcat {
      case Left((page, error)) =>
        reporter ! Message.Failure(page, error)
        Nil

      case Right(t) =>
        List(t)
    }

  def pageFlow[T](reporter: ActorRef[Message.Failure]): Flow[(Page, Either[Throwable, T]), (Page, T), _] =
    Flow[(Page, Either[Throwable, T])].mapConcat {
      case (page, Left(error)) =>
        reporter ! Message.Failure(page, error)
        Nil

      case (page, Right(t)) =>
        List((page, t))
    }
}
