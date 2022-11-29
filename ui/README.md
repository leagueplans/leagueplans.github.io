# ui
This project uses
- [Scala.js](https://www.scala-js.org/), as the language of choice.
- [React](https://reactjs.org/), as the core framework.
- [scalajs-react](https://github.com/japgolly/scalajs-react), for defining React components in Scala.js.
- [sbt](https://www.scala-sbt.org/), for building the Scala half of the project, which will produce Javascript modules. You'll need to install this separately.
- [npm](https://www.npmjs.com/), for Javascript dependency management. You'll also need to install this separately.
- [Vite](https://vitejs.dev/), for both its [HMR](https://vitejs.dev/guide/features.html#hot-module-replacement) capabilities, and building the final project.

## Development
### Setup
- Install npm & sbt. You may need to take additional steps (like adding the location of their scripts to your `PATH`) in order to run these commands in the local directory.
- Run `npm install` in this directory. This will download the Javascript dependencies defined in [`package.json`](package.json).

### Building
#### For local development 
Run `npm run dev` in this directory.

This will start up a local server for the project. The server uses [HMR](https://vitejs.dev/guide/features.html#hot-module-replacement), such that you may make changes to the local files and those changes will be instantly propagated through the server.

In order to propagate live changes to Scala files, you will need to run `sbt ~ui/fastLinkJS` from the project root, and in a separate terminal. This will rerun the linking process every time a Scala file is saved, which allows the local server to detect when a change is made to a Scala file.

#### For production
Run `npm run build` in this directory.

This will produce an optimised bundle under [`target/vite`](target/vite) suitable for deployment. You can run `npm run preview` to start up a local server to preview this bundle. The server does not use HMR when previewing production builds.

#### Implementation
The npm commands above defer (via [`package.json`](package.json)) to Vite. Vite's build have been configured (via [`vite.config.js`](vite.config.js)) to trigger an sbt build first, which ensures that the produced bundles are up-to-date with the local repository. The configuration for this is a slightly modified version of the code demonstrated by [Sébastien Doeraene](https://github.com/sjrd) in his talk, [Getting Started with Scala.js and Vite](https://www.youtube.com/watch?v=dv7fPmgFTNA). The repository featured in his talk can be found [here](https://github.com/sjrd/scalajs-sbt-vite-laminar-chartjs-example).

### Component design
A typical component implementation will look something like this:
```scala
import MyComponent.{Backend, Props, State}
import japgolly.scalajs.react.component.Scala.{Component, Unmounted}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react.{BackendScope, CtorType, ScalaComponent}

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

final class MyComponent(initialStateParam: Any) {
  private val build: Component[Props, State, Backend, CtorType.Props] =
    ScalaComponent
      .builder[Props]
      .initialState[State](State(initialStateParam))
      .renderBackend[Backend]
      .build

  def apply(propsParam: Any): Unmounted[Props, State, Backend] =
    build(Props(propsParam))
}

object MyComponent {
  @js.native @JSImport("/styles/myComponent.module.css", JSImport.Default)
  private object Styles extends js.Object {
    val myStyle: String = js.native
  }
  
  final case class Props(propsParam: Any)
  final case class State(stateParam: Any)
  
  final class Backend(scope: BackendScope[Props, State]) {
    private val subComponent = new MyComponent()
    
    def render(props: Props, state: State): VdomNode =
      ???
  }
}
```

#### The `build` val
There are a few key conventions to a React component's lifecycle:
- a mount event should be fired when a component is added to the virtual DOM
- update events should be fired when a component's props or state change
- an unmount event should be fired when a component is removed from the virtual DOM

The `build` val is important for making sure that our component implementations respect these conventions. For example, on the first invocation of the `apply` method we expect a mount event to be fired, and on subsequent invocations we expect an update event to be fired. If `build` was implemented as a def rather than a val, then a new component instance would be generated on every call to `apply`. Instead of getting an update event on subsequent invocations, we'd actually get a mount event for the new component instance, and an unmount event for the prior component instance.

If there are no parameters needed to define `build`, then we can remove the `MyComponent` class and move both `build` and `apply` to the companion object.

#### The `Styles` object

#### State management

#### Subcomponents
