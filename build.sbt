name := "osrs-planner"

ThisBuild / scalaVersion := "3.0.0"

lazy val root =
  (project in file("."))
    .aggregate(ui)

lazy val ui =
  (project in file("ui"))
    .enablePlugins(ScalaJSBundlerPlugin)
    .settings(
      libraryDependencies ++= List(
        ("org.scala-js" %%% "scalajs-dom" % "1.1.0").cross(CrossVersion.for3Use2_13),
        ("com.github.japgolly.scalajs-react" %%% "core" % "1.7.7").cross(CrossVersion.for3Use2_13)
      ),
      Compile / npmDependencies ++= List(
        "react" -> "17.0.2",
        "react-dom" -> "17.0.2"
      ),
      scalaJSUseMainModuleInitializer := true,
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
    )
