import org.scalajs.linker.interface.ModuleSplitStyle

name := "league-plans"

ThisBuild / scalaVersion := "3.4.0"
ThisBuild / scalacOptions ++= List(
  "-deprecation",
  "-encoding", "utf-8",
  "-explain-types",
  "-feature",
  "-unchecked",
  "-Wunused:all",
  "-Wvalue-discard",
  "-Ysafe-init"
)

lazy val root =
  (project in file("."))
    .aggregate(common.jvm, common.js, wikiScraper, ui)

val circeVersion = "0.14.6"

lazy val codec =
  crossProject(JVMPlatform, JSPlatform).in(file("codec"))

lazy val common =
  crossProject(JVMPlatform, JSPlatform).in(file("common"))
    .settings(
      libraryDependencies ++= List(
        "io.circe" %%% "circe-core" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion,
        "io.circe" %%% "circe-parser" % circeVersion
      )
    )
    .dependsOn(codec)

val akkaVersion = "2.6.18"
val scrimageVersion = "4.1.1"

lazy val wikiScraper =
  project.in(file("scraper"))
    .settings(
      libraryDependencies ++= List(
        "ch.qos.logback" % "logback-classic" % "1.4.14",
        "org.log4s" %% "log4s" % "1.10.0",
        "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
        ("com.typesafe.akka" %% "akka-http" % "10.2.9").cross(CrossVersion.for3Use2_13),
        "org.parboiled" %% "parboiled" % "2.5.1",
        "com.sksamuel.scrimage" % "scrimage-core" % scrimageVersion,
        ("com.sksamuel.scrimage" %% "scrimage-scala" % scrimageVersion).cross(CrossVersion.for3Use2_13)
      )
    )
    .dependsOn(common.jvm)

val fastLinkOutputDir = taskKey[String]("output directory for `npm run dev`")
val fullLinkOutputDir = taskKey[String]("output directory for `npm run build`")

// Vite outputs a warning about sourcemaps. I don't know why, since the browser can
// find and use the sourcemaps correctly. I did an investigation and wrote up a
// summary here:
// https://github.com/scala-js/vite-plugin-scalajs/issues/4#issuecomment-1771614021
lazy val ui =
  project.in(file("ui"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      // Needed for deriving a codec for StorageWorkerProtocol.ToClient
      scalacOptions ++= List("-Xmax-inlines", "40"),
      libraryDependencies ++= List(
        "org.scala-js" %%% "scalajs-dom" % "2.8.0",
        ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13),
        "com.raquo" %%% "laminar" % "16.0.0",
        "io.circe" %%% "circe-scalajs" % circeVersion
      ),
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= (config =>
        config
          .withModuleKind(ModuleKind.ESModule)
          .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("ddm.ui")))
      ),
      fastLinkOutputDir := {
        // Ensure that fastLinkJS has run, then return its output directory
        (Compile / fastLinkJS).value
        (Compile / fastLinkJS / scalaJSLinkerOutputDirectory).value.getAbsolutePath
      },
      fullLinkOutputDir := {
        // Ensure that fullLinkJS has run, then return its output directory
        (Compile / fullLinkJS).value
        (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value.getAbsolutePath
      }
    )
    .dependsOn(common.js)
