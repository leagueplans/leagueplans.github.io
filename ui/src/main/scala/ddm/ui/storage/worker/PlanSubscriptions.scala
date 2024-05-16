package ddm.ui.storage.worker

import ddm.ui.storage.model.LamportTimestamp.increment
import ddm.ui.storage.model.{LamportTimestamp, PlanID}
import ddm.ui.storage.worker.PlanSubscriptions.{Port, Subscriptions}
import ddm.ui.wrappers.workers.MessagePortClient

import scala.collection.mutable

object PlanSubscriptions {
  type Port[Out, In] = MessagePortClient[Out, In]
  type Subscriptions[Out, In] = (LamportTimestamp, Set[Port[Out, In]])
  
  def empty[Out, In]: PlanSubscriptions[Out, In] =
    mutable.Map.empty
}

opaque type PlanSubscriptions[Out, In] = mutable.Map[PlanID, Subscriptions[Out, In]]

extension [Out, In](self: PlanSubscriptions[Out, In]) {
  def register(port: Port[Out, In], planID: PlanID): LamportTimestamp =
    get(planID) match {
      case Some((lamport, ports)) =>
        self.update(planID, (lamport, ports + port))
        lamport

      case None =>
        val lamport = LamportTimestamp.initial
        self += planID -> (lamport, Set(port))
        lamport
    }

  def deregister(port: Port[Out, In], planID: PlanID): Unit =
    get(planID).foreach { (lamport, ports) =>
      val updatedPorts = ports - port
      if (updatedPorts.isEmpty)
        self -= planID
      else 
        self.update(planID, (lamport, updatedPorts))
    }
    
  def all: List[(PlanID, (LamportTimestamp, Set[Port[Out, In]]))] =
    self.toList
    
  def get(planID: PlanID): Option[(LamportTimestamp, Set[Port[Out, In]])] =
    self.get(planID)
    
  def incrementLamport(planID: PlanID): Unit =
    get(planID).foreach { (lamport, ports) =>
      self.update(planID, (lamport.increment, ports))
    }
}


