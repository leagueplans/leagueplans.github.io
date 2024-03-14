package ddm.scraper.main.runner

import akka.actor.typed.{ActorRef, Behavior}

type Spawn[T] = Behavior[T] => ActorRef[T]
