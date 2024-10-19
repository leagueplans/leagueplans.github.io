package ddm.ui.wrappers.animation

import ddm.ui.facades.animation.{Animatable, Animation, KeyframeAnimationOptions}
import org.scalajs.dom.Element

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.scalajs.js
import scala.scalajs.js.JSConverters.{JSRichIterable, iterableOnceConvertible2JSRichIterableOnce}

extension (self: Element) {
  def animate(
    duration: Duration,
    keyframe1: Iterable[KeyframePropertySetter[?]],
    keyframeN: Iterable[KeyframePropertySetter[?]]*
  ): Animation =
    animate(
      toJS((keyframe1 +: keyframeN).toArray),
      duration.toUnit(TimeUnit.MILLISECONDS)
    )

  def animate(
    options: KeyframeAnimationOptions,
    keyframe1: Iterable[KeyframePropertySetter[?]],
    keyframeN: Iterable[KeyframePropertySetter[?]]*
  ): Animation =
    animate(
      toJS((keyframe1 +: keyframeN).toArray),
      options
    )

  def animate(
    duration: Duration,
    setter1: KeyframesPropertySetter[?],
    setterN: KeyframesPropertySetter[?]*
  ): Animation =
    animate(
      toJS((setter1 +: setterN).toArray),
      duration.toUnit(TimeUnit.MILLISECONDS)
    )

  def animate(
    options: KeyframeAnimationOptions,
    setter1: KeyframesPropertySetter[?],
    setterN: KeyframesPropertySetter[?]*
  ): Animation =
    animate(
      toJS((setter1 +: setterN).toArray),
      options
    )

  private def animate(
    keyframes: js.UndefOr[js.Object],
    options: Double | KeyframeAnimationOptions
  ): Animation =
    self.asInstanceOf[Animatable].animate(keyframes, options)

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
