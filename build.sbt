import org.scalajs.linker.interface.ESVersion

name := "league-plans"

ThisBuild / scalaVersion := "3.9.0"
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
    .aggregate(
      codec.jvm, codec.js,
      common.jvm, common.js,
      wikiScraper,
      mapRenderer,
      uicommon,
      ui,
      taskimporter,
      scrapereview
    )

lazy val codec =
  crossProject(JVMPlatform, JSPlatform).in(file("codec"))
    .settings(
      libraryDependencies ++= List(
        "org.scalatest" %%% "scalatest" % "3.2.20" % "test",
        "org.scalatestplus" %%% "scalacheck-1-19" % "3.2.20.0" % "test"
      )
    )

val circeVersion = "0.14.16"

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

val logbackVersion = "1.6.4"
val zioVersion = "2.1.26"
val zioLoggingVersion = "2.5.3"

lazy val wikiScraper =
  project.in(file("scraper"))
    .settings(
      libraryDependencies ++= List(
        "ch.qos.logback" % "logback-classic" % logbackVersion,
        "dev.zio" %% "zio" % zioVersion,
        "dev.zio" %% "zio-streams" % zioVersion,
        "dev.zio" %% "zio-http" % "3.11.6",
        "dev.zio" %% "zio-logging" % zioLoggingVersion,
        "dev.zio" %% "zio-logging-slf4j2" % zioLoggingVersion,
        "org.parboiled" %% "parboiled" % "2.5.1"
      )
    )
    .dependsOn(common.jvm)

/** Renders the map's tiles from the game cache, for publishing to the map-tiles repository.
  *
  * A JVM tool. Nothing in it reaches the app's bundle.
  */
lazy val mapRenderer =
  project.in(file("maprender"))
    .settings(
      resolvers += "RuneLite" at "https://repo.runelite.net",
      libraryDependencies ++= List(
        // Pinned: a game update can change the cache format, so this is bumped deliberately.
        // Its slf4j-simple is swapped for logback, which can filter out its noisiest warning.
        ("net.runelite" % "cache" % "1.13.6").exclude("org.slf4j", "slf4j-simple"),
        "ch.qos.logback" % "logback-classic" % logbackVersion
      )
    )
    .dependsOn(common.jvm % "compile->compile;test->test")

val fullLinkOutputDir = taskKey[String]("output directory for `npm run build`")

lazy val scalaJSSettings = List(
  scalaJSLinkerConfig ~= (
    _.withModuleKind(ModuleKind.ESModule)
      .withESFeatures(_.withESVersion(ESVersion.ES2017))
  )
)

lazy val viteSettings = List(
  scalaJSUseMainModuleInitializer := true,
  // `npm run dev` reads the bundle from here without asking sbt, because a second sbt can't
  // start while another is still loading. vite.shared.js names the same directory.
  Compile / fastLinkJS / scalaJSLinkerOutputDirectory := target.value / "fastopt",
  fullLinkOutputDir := {
    // Ensure that fullLinkJS has run, then return its output directory
    (Compile / fullLinkJS).value
    (Compile / fullLinkJS / scalaJSLinkerOutputDirectory).value.getAbsolutePath
  }
)

/** Browser façades, their wrappers, and the Laminar components built on top of them.
  *
  * Holds nothing that knows about plans, players or scrapes, so both the app and the tools
  * that maintain its data can build on it.
  */
lazy val uicommon =
  project.in(file("uicommon"))
    .enablePlugins(ScalaJSPlugin)
    .settings(
      scalaJSSettings,
      libraryDependencies ++= List(
        "org.scala-js" %%% "scalajs-dom" % "2.8.1",
        ("org.scala-js" %%% "scalajs-java-securerandom" % "1.0.0").cross(CrossVersion.for3Use2_13),
        "org.scala-js" %%% "scala-js-macrotask-executor" % "1.1.1",
        "com.raquo" %%% "laminar" % "17.2.1",
        "io.circe" %%% "circe-core" % circeVersion,
        "io.circe" %%% "circe-parser" % circeVersion,
        "io.circe" %%% "circe-scalajs" % circeVersion
      )
    )
    .dependsOn(codec.js % "compile->compile;test->test")

lazy val ui =
  project.in(file("ui"))
    .enablePlugins(ScalaJSPlugin)
    .settings(scalaJSSettings, viteSettings)
    .dependsOn(codec.js % "test->test", common.js, uicommon)

/** A tool for reconciling a new league's task list against the one already published. */
lazy val taskimporter =
  project.in(file("taskimporter"))
    .enablePlugins(ScalaJSPlugin)
    .settings(scalaJSSettings, viteSettings)
    .dependsOn(common.js, uicommon)

/** A tool for reviewing what a scrape changed before any of it reaches the app. */
lazy val scrapereview =
  project.in(file("scrapereview"))
    .enablePlugins(ScalaJSPlugin)
    .settings(scalaJSSettings, viteSettings)
    .dependsOn(codec.js % "test->test", common.js, uicommon)
