package ddm.scraper.main

import akka.actor.typed.{ActorRef, Behavior}

package object runner {
  type Spawn[T] = Behavior[T] => ActorRef[T]
}
