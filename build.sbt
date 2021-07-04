name := "osrs-planner"

ThisBuild / scalaVersion := "2.13.6"

lazy val root =
  (project in file("."))
    .aggregate(ui)

lazy val ui =
  (project in file("ui"))
    .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
    .settings(
      libraryDependencies ++= List(
        "org.scala-js" %%% "scalajs-dom" % "1.1.0",
        "com.github.japgolly.scalajs-react" %%% "core" % "1.7.7"
      ),
      Compile / npmDependencies ++= List(
        "react" -> "17.0.2",
        "react-dom" -> "17.0.2"
      ),
      scalaJSUseMainModuleInitializer := true,
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      Test / requireJsDomEnv := true,
      webpack / version := "4.44.2",
      startWebpackDevServer / version := "3.11.2"
    )
