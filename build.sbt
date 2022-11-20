name := "osrs-planner"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / scalacOptions ++= List(
  "-encoding", "utf-8",                // Specify character encoding used by source files.
  "-explaintypes",                     // Explain type errors in more detail.
  "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
  "-language:higherKinds",             // Allow higher-kinded types
  "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
  "-Wdead-code",                       // Warn when dead code is identified.
  "-Wextra-implicit",                  // Warn when more than one implicit parameter section is defined.
  "-Wnumeric-widen",                   // Warn when numerics are widened.
  "-Wunused:imports",                  // Warn if an import selector is not referenced.
  "-Wunused:locals",                   // Warn if a local definition is unused.
  "-Wunused:patvars",                  // Warn if a variable bound in a pattern is unused.
  "-Wunused:privates",                 // Warn if a private member is unused.
  "-Wvalue-discard",                   // Warn when non-Unit expression results are unused.
  "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
  "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
  "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
  "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
  "-Xlint:deprecation",                // Emit warning and location for usages of deprecated APIs.
  "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
  "-Xlint:option-implicit",            // Option.apply used implicit view.
  "-Xlint:package-object-classes",     // Class or object defined in package object.
  "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow"       // A local type parameter shadows a type already in scope.
)

lazy val root =
  (project in file("."))
    .aggregate(common.jvm, common.js, wikiScraper, ui)

val circeVersion = "0.14.1"

lazy val common =
  crossProject(JVMPlatform, JSPlatform).in(file("common"))
    .settings(
      libraryDependencies ++= List(
        "io.circe" %%% "circe-core" % circeVersion,
        "io.circe" %%% "circe-generic" % circeVersion,
        "io.circe" %%% "circe-parser" % circeVersion
      )
    )

val akkaVersion = "2.6.18"
val scrimageVersion = "4.0.31"

lazy val wikiScraper =
  project.in(file("scraper"))
    .settings(
      libraryDependencies ++= List(
        "ch.qos.logback" % "logback-classic" % "1.2.11",
        "org.log4s" %% "log4s" % "1.10.0",
        "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-stream-typed" % akkaVersion,
        "com.typesafe.akka" %% "akka-http" % "10.2.9",
        "org.parboiled" %% "parboiled" % "2.4.0",
        "com.sksamuel.scrimage" % "scrimage-core" % scrimageVersion,
        "com.sksamuel.scrimage" %% "scrimage-scala" % scrimageVersion
      )
    )
    .dependsOn(common.jvm)

val reactVersion = "17.0.2"

lazy val ui =
  project.in(file("ui"))
    .enablePlugins(ScalaJSPlugin, ScalaJSBundlerPlugin)
    .settings(
      libraryDependencies ++= List(
        "org.scala-js" %%% "scala-js-macrotask-executor" % "1.0.0",
        "org.scala-js" %%% "scalajs-dom" % "2.1.0",
        "com.github.japgolly.scalajs-react" %%% "core" % "2.0.1",
        "io.circe" %%% "circe-scalajs" % circeVersion
      ),
      Compile / npmDependencies ++= List(
        "react" -> reactVersion,
        "react-dom" -> reactVersion,
        "fuse.js" -> "6.5.3"
      ),
      scalaJSUseMainModuleInitializer := true,
      jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
      Test / requireJsDomEnv := true,
      installJsdom / version := "16.6.0",
      webpack / version := "4.46.0",
      startWebpackDevServer / version := "3.11.2"
    )
    .dependsOn(common.js)
