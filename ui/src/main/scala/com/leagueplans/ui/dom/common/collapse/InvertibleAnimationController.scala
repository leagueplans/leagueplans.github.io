package com.leagueplans.ui.dom.common.collapse

import com.leagueplans.ui.dom.common.collapse.InvertibleAnimationController.Status
import com.leagueplans.ui.wrappers.animation.Animation
import com.raquo.airstream.state.{StrictSignal, Var}
import com.raquo.laminar.api.{L, enrichSource, seqToModifier}

import scala.concurrent.duration.Duration

object InvertibleAnimationController {
  sealed trait Status

  object Status {
    sealed trait Idle extends Status
    case object Open extends Idle
    case object Closed extends Idle

    sealed trait Animating extends Status
    case object Opening extends Animating
    case object Closing extends Animating
  }
}

final class InvertibleAnimationController(startOpen: Boolean, animationDuration: Duration) {
  private val status = Var[Status](if (startOpen) Status.Open else Status.Closed).distinct
  val statusSignal: StrictSignal[Status] = status.signal
  private val onAnimating = statusSignal.changes.collect { case direction: Status.Animating => direction }
  private var members = Set.empty[L.Element]
  private var activeAnimations = Map.empty[L.Element, Animation.Instance]

  def toggle(): Unit =
    status.update {
      case Status.Open if members.isEmpty => Status.Closed
      case Status.Open | Status.Opening => Status.Closing
      case Status.Closed if members.isEmpty => Status.Open
      case Status.Closed | Status.Closing => Status.Opening
    }

  def open(): Unit =
    status.update {
      case Status.Closed if members.isEmpty => Status.Open
      case Status.Closed | Status.Opening | Status.Closing => Status.Opening
      case Status.Open => Status.Open
    }

  def close(): Unit =
    status.update {
      case Status.Open if members.isEmpty => Status.Closed
      case Status.Open | Status.Opening | Status.Closing => Status.Closing
      case Status.Closed => Status.Closed
    }
    
  def isOpen: Boolean =
    status.now() == Status.Open
    
  def isClosed: Boolean =
    status.now() == Status.Closed

  def apply(
    toOpen: Duration => Animation,
    toClose: Duration => Animation,
  ): L.Modifier[L.Element] = {
    List(
      mountCallbacks,
      L.inContext(member => onAnimating --> (direction => 
        setAnimationDirection(
          member,
          direction,
          toOpen(animationDuration),
          toClose(animationDuration)
        )
      ))
    )
  }
  
  private val mountCallbacks: L.Modifier[L.Element] =
    L.onMountUnmountCallback(
      mount = member => members += member.thisNode,
      unmount = member => {
        stopTrackingAnimation(member)
        members -= member
      }
    )
  
  private def setAnimationDirection(
    member: L.Element,
    direction: Status.Animating,
    open: => Animation,
    close: => Animation
  ): Unit =
    activeAnimations.get(member) match {
      // We have .distinct on the Var, so the status must be different. This
      // ensures we must be changing direction if we already have an animation.
      case Some(animation) =>
        animation.reverse()

      case None =>
        val animation = direction match {
          case Status.Opening => open
          case Status.Closing => close
        }
        animate(member, animation)
    }

  private def animate(member: L.Element, animation: Animation): Unit = {
    val instance = animation.play(member)
    activeAnimations += (member -> instance)
    instance.onfinish = { _ =>
      instance.commitStyles()
      instance.cancel()
      stopTrackingAnimation(member)
    }
  }

  private def stopTrackingAnimation(member: L.Element): Unit = {
    activeAnimations -= member
    if (activeAnimations.isEmpty)
      status.update {
        case Status.Opening => Status.Open
        case Status.Closing => Status.Closed
        case idle: Status.Idle => idle
      }
  }
}
