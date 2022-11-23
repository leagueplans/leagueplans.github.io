package ddm.ui

import japgolly.scalajs.react.vdom.{TagMod, VdomNode}

package object component {
  /* Typically with react, state is managed by the first common ancestor of
   * all components which need the state. A setter is then passed down the
   * hierarchy to the component that can manipulate the state.
   *
   * I'm doing something different. It remains to be seen as to whether this
   * is a good idea.
   *
   * In my world, state lives in the component responsible for modifying the
   * state. That component takes, as part of its props, an instance of type
   * ===================
   * (T, TagMod) => VdomNode
   * ===================
   * where
   * - the argument T corresponds to the public state
   * - the argument TagMod corresponds to any dom elements created by the
   *   component
   *
   * The render function of the component will then look like
   * ===================
   * def render(props: Props, state: State): VdomNode = {
   *   val content = <.div(...)
   *   props.render(state.t, content)
   * }
   * ===================
   * A parent would then use this stateful component as follows:
   * ===================
   * def render(props: Props): VdomNode =
   *   withState(...) { case (state, content) =>
   *     <.div(...)
   *   }
   *
   * private def withState(...): Render[T] => VdomNode =
   *   render => StatefulComponent.build(StatefulComponent.props(
   *     ...,
   *     render
   *   ))
   * ===================
   * The withX function provides a nice syntax for composing such stateful
   * components.
   *
   * My suggested benefit of this approach comes from cases where there is
   * only one way of updating the state (e.g. input types). When the
   * components for such cases are written following the above guidelines,
   * it becomes possible for the components to handle the state management
   * themselves. This removes the need for arbitrary ancestor components
   * to do state management, or for the state management to be
   * reimplemented when new dependencies on the state are introduced.
   */
  type Render[-T] = RenderE[T, TagMod]
  type With[+T] = WithE[T, TagMod]
  type RenderE[-T, -E <: TagMod] = (T, E) => VdomNode
  type WithE[+T, +E <: TagMod] = RenderE[T, E] => VdomNode
}
