# ui
This project uses
- [Scala](https://www.scala-lang.org/), as the language of choice.
- [Scala.js](https://www.scala-js.org/), for transpiling our Scala code into Javascript.
- [Laminar](https://laminar.dev/), as the core UI framework for constructing the DOM and responding to user input.
- [sbt](https://www.scala-sbt.org/), for building the Scala half of the project, which will produce Javascript modules.
- [npm](https://www.npmjs.com/), for Javascript dependency management.
- [Vite](https://vitejs.dev/), for building the final project.

## Development
### Setup
- Install npm & sbt. You may need to take additional steps (like adding the location of their scripts to your `PATH`) in order to run these commands from the local directory.
- Run `npm install` from this directory. This will download the Javascript dependencies defined in [`package.json`](package.json).

### Building
#### For local development 
Run `npm run dev` from this directory.

This will start up a local server for the project. In order to propagate live changes from Scala files, you will need to run `sbt ~ui/fastLinkJS` from the project root in a separate terminal. This will rerun the linking process every time a Scala file is saved, which allows the local server to detect when a change is made to a Scala file.

#### For production
Run `npm run build` from this directory.

This will produce an optimised bundle under [`target/vite`](target/vite) suitable for deployment. You can run `npm run preview` to start up a local server to preview this bundle.

#### Implementation
The npm commands above defer (via [`package.json`](package.json)) to Vite. Vite's build has been configured (via [`vite.config.js`](vite.config.js)) to trigger an sbt build first, which ensures that the produced bundles are up-to-date with the local repository. The configuration for this is a slightly modified version of the code demonstrated by [Sébastien Doeraene](https://github.com/sjrd) in his talk, [Getting Started with Scala.js and Vite](https://www.youtube.com/watch?v=dv7fPmgFTNA). The repository featured in his talk can be found [here](https://github.com/sjrd/scalajs-sbt-vite-laminar-chartjs-example).
