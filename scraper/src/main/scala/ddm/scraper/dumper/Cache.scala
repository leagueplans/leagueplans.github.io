package ddm.scraper.dumper

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors

import scala.util.Try

object Cache {
  enum Message[+T] {
    case NewEntry(entry: T)
    case Complete(runStatus: Try[?])
  }

  def init[T](onComplete: (Try[?], List[T]) => Unit): Behavior[Message[T]] =
    behavior[T](onComplete, acc = List.empty)

  private def behavior[T](
    onComplete: (Try[?], List[T]) => Unit,
    acc: List[T]
  ): Behavior[Message[T]] =
    Behaviors.receiveMessage {
      case Message.NewEntry(entry) =>
        behavior(onComplete, acc :+ entry)

      case Message.Complete(runStatus) =>
        onComplete(runStatus, acc)
        Behaviors.stopped
    }
}
