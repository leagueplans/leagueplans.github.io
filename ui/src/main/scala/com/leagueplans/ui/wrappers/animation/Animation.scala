package com.leagueplans.ui.wrappers.animation

import com.leagueplans.ui.facades.animation.{Animatable, KeyframeAnimationOptions, Animation as AnimationFacade}
import com.raquo.laminar.nodes.ReactiveElement

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichIterable, iterableOnceConvertible2JSRichIterableOnce}

object Animation {
  type Instance = AnimationFacade

  def apply(
    duration: Duration,
    keyframe1: Iterable[KeyframePropertySetter[?]],
    keyframeN: Iterable[KeyframePropertySetter[?]]*
  ): Animation =
    new Animation(toJS((keyframe1 +: keyframeN).toArray), duration.toUnit(TimeUnit.MILLISECONDS))

  def apply(
    options: KeyframeAnimationOptions,
    keyframe1: Iterable[KeyframePropertySetter[?]],
    keyframeN: Iterable[KeyframePropertySetter[?]]*
  ): Animation =
    new Animation(toJS((keyframe1 +: keyframeN).toArray), options)

  def apply(
    duration: Duration,
    setter1: KeyframesPropertySetter[?],
    setterN: KeyframesPropertySetter[?]*
  ): Animation =
    new Animation(toJS((setter1 +: setterN).toArray), duration.toUnit(TimeUnit.MILLISECONDS))

  def apply(
    options: KeyframeAnimationOptions,
    setter1: KeyframesPropertySetter[?],
    setterN: KeyframesPropertySetter[?]*
  ): Animation =
    new Animation(toJS((setter1 +: setterN).toArray), options)

  private def toJS(frames: Array[Iterable[KeyframePropertySetter[?]]]): js.Array[js.Dictionary[?]] =
    frames.map(frame =>
      js.Object.fromEntries(
        frame.map(setter =>
          js.Tuple2(setter.property.name, setter.value)
        ).toJSIterable
      )
    ).toJSArray

  private def toJS(setters: Iterable[KeyframesPropertySetter[?]]): js.Object =
    js.Object.fromEntries(
      setters.map(setter =>
        js.Tuple2(setter.property.name, setter.values.toJSArray)
      ).toJSIterable
    ).asInstanceOf[js.Object]
}

final class Animation(keyframes: js.UndefOr[js.Object], options: Double | KeyframeAnimationOptions) {
  def play(element: ReactiveElement[?]): Animation.Instance =
    element.ref.asInstanceOf[Animatable].animate(keyframes, options)
}
