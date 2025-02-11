package com.leagueplans.scraper.main

import com.leagueplans.scraper.http.HTTPClient
import com.leagueplans.scraper.main.runner.ScrapeRunner
import com.leagueplans.scraper.reporter.RunReporter
import com.leagueplans.scraper.wiki.http.WikiClient
import zio.http.Header.UserAgent
import zio.http.URL
import zio.logging.backend.SLF4J
import zio.{Console, Duration, RIO, Runtime, Schedule, Scope, Task, Trace, URIO, ZIO, ZIOAppArgs, ZIOAppDefault, ZLayer}

import java.nio.file.{Files, Path}
import scala.concurrent.duration.DurationInt
import scala.util.Try

object Main extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  def run: ZIO[ZIOAppArgs & Scope, Throwable, Unit] =
    for {
      args <- CommandLineArgs.parse
      baseURL <- ZIO.fromEither(URL.decode("https://oldschool.runescape.wiki"))
      targetDirectory <- makeTargetDirectory(args)
      reporter <- makeReporter(baseURL, targetDirectory)
      httpClient <- makeHTTPClient
      runner <- ScrapeRunner.make(args, targetDirectory, makeWikiClient(httpClient, baseURL))
      console <- ZIO.console
      _ <- startBackgroundMetricDumping(console)
      result <- runner.run.exit
      _ <- ZIO.fromTry(reporter.report(result))
      _ <- result
    } yield ()

  private def makeTargetDirectory(args: CommandLineArgs)(using Trace): Task[Path] =
    for {
      path <- ZIO.fromTry(args.get("target-directory")(directory => Try(Path.of(directory))))
      _ <- ZIO.attempt(Files.createDirectories(path))
    } yield path

  private def makeReporter(baseURL: URL, targetDirectory: Path)(using Trace): Task[RunReporter] =
    ZIO.fromTry(
      for {
        target <- Try(targetDirectory.resolve("report.md"))
        reporter <- RunReporter.make(baseURL, target)
      } yield reporter
    )

  private def makeHTTPClient(using Trace): RIO[Scope, HTTPClient] =
    HTTPClient.make(
      name = "main",
      connectionPoolSize = 4,
      idleTimeout = 30.seconds,
      maxRequestBurst = 10,
      rateLimitInterval = 100.milliseconds
    )

  private def makeWikiClient(httpClient: HTTPClient, baseURL: URL): WikiClient =
    new WikiClient(
      httpClient,
      UserAgent(
        UserAgent.ProductOrComment.Product("leagueplansbot", version = None),
        List(UserAgent.ProductOrComment.Comment("+https://github.com/leagueplans/leagueplans.github.io"))
      ),
      baseURL,
      pageLimit = 50,
      // Recurs at 5s, 10s, 20s, 35s, 1m, 1m40s, 2m45s
      retrySchedule = Schedule.fibonacci(Duration.fromScala(5.seconds)).jittered && Schedule.recurs(7)
    )

  private def startBackgroundMetricDumping(console: Console)(using Trace): URIO[Scope, Unit] = {
    val dump = for {
      _ <- console.printLine("Dumping metrics")
      snapshot <- ZIO.metrics
      _ <- snapshot.dump
    } yield ()

    dump
      .repeat(Schedule.fixed(Duration.fromScala(15.seconds)))
      .forkScoped
      .unit
  }
}
