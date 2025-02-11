import org.scalajs.linker.interface.ModuleSplitStyle

name := "league-plans"

ThisBuild / scalaVersion := "3.6.3"
ThisBuild / scalacOptions ++= List(
  "-deprecation",
  "-encoding", "utf-8",
  "-feature",
  "-unchecked",
  "-Wsafe-init",
  "-Wunused:all",
  "-Wvalue-discard"
)

lazy val root =
  (project in file("."))
    .aggregate(codec.jvm, codec.js, common.jvm, common.js, wikiScraper, ui)

lazy val codec =
  crossProject(JVMPlatform, JSPlatform).in(file("codec"))
    .settings(
      libraryDependencies ++= List(
        "org.scalatest" %%% "scalatest" % "3.2.19" % "test",
        "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % "test"
      )
    )

val circeVersion = "0.14.10"

lazy val common =
  crossProject(JVMPlatform, JSPlatform).in(file("common"))
    .settings(
      libraryDependencies ++= List(
        "io.circe" %%% "circe-core" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion,
        "io.circe" %%% "circe-parser" % circeVersion
      )
    )
    .dependsOn(codec % "compile->compile;test->test")

val scrimageVersion = "4.3.0"
val zioVersion = "2.1.15"
val zioLoggingVersion = "2.4.0"

lazy val wikiScraper =
  project.in(file("scraper"))
    .settings(
      libraryDependencies ++= List(
        "ch.qos.logback" % "logback-classic" % "1.5.16",
        "dev.zio" %% "zio" % zioVersion,
        "dev.zio" %% "zio-streams" % zioVersion,
        "dev.zio" %% "zio-http" % "3.0.1",
        "dev.zio" %% "zio-logging" % zioLoggingVersion,
        "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion,
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
      libraryDependencies ++= List(
        "org.scala-js" %%% "scalajs-dom" % "2.8.0",
        ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13),
        "com.raquo" %%% "laminar" % "17.2.0",
        "io.circe" %%% "circe-scalajs" % circeVersion
      ),
      scalaJSUseMainModuleInitializer := true,
      scalaJSLinkerConfig ~= (config =>
        config
          .withModuleKind(ModuleKind.ESModule)
          .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("com.leagueplans.ui")))
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
    .dependsOn(
      common.js,
      codec.js % "test->test"
    )
