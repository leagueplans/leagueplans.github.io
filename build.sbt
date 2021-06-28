name := "osrs-planner"

ThisBuild / scalaVersion := "3.0.0"

lazy val root =
  (project in file("."))
    .aggregate(ui)

lazy val ui =
  (project in file("ui"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      libraryDependencies += ("org.scala-js" %%% "scalajs-dom" % "1.1.0").cross(CrossVersion.for3Use2_13),
      scalaJSUseMainModuleInitializer := true,
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv()
    )
