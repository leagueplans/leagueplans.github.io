package ddm.scraper.main

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, ActorSystem, Behavior, Terminated}
import akka.http.scaladsl.model.headers.`User-Agent`
import ddm.scraper.dumper.Cache
import ddm.scraper.http.ThrottledHttpClient
import ddm.scraper.main.runner.{Runner, ScrapeItemsRunner, ScrapeSkillIconsRunner}
import ddm.scraper.reporter.ReportPrinter
import ddm.scraper.wiki.http.MediaWikiClient
import ddm.scraper.wiki.model.Page
import org.log4s.getLogger

import java.nio.file.{Files, Path}
import scala.annotation.nowarn
import scala.concurrent.Await
import scala.concurrent.duration.DurationInt
import scala.util.chaining.scalaUtilChainingOps
import scala.util.{Failure, Success}

object Main extends App {
  private val actorSystem = ActorSystem[Nothing](
    Behaviors.setup[Nothing] { context =>
      import context.{executionContext, system}

      def spawn[T](behavior: Behavior[T]): ActorRef[T] = {
        val ref = context.spawnAnonymous(behavior)
        context.watch(ref)
        ref
      }

      val baseURL = "https://oldschool.runescape.wiki"
      val clArgs = CommandLineArgs.parse(args)
      val runner = Runner.from(clArgs)
      val userAgent = clArgs.get("user-agent")(`User-Agent`(_))
      val reportFile = clArgs.get("target-directory")(Path.of(_).resolve("report.md"))

      val client = new MediaWikiClient(
        new ThrottledHttpClient(
          maxThroughput = 5,
          interval = 1.second,
          bufferSize = Int.MaxValue,
          parallelism = 4
        ),
        userAgent,
        baseURL
      )

      val reporter =
        Cache
          .init[(Page, Throwable)] { (runStatus, failures) =>
            val logger = getLogger("Reporter")
            runStatus match {
              case Failure(error) => logger.error(error)("Run failed")
              case Success(_) => logger.info("Run succeeded")
            }

            val report = ReportPrinter.print(runStatus, failures, baseURL)
            Files.write(reportFile, report.getBytes): @nowarn("msg=discarded non-Unit value")
          }
          .pipe(spawn)

      runner match {
        case r: ScrapeItemsRunner => r.run(client, reporter, spawn, spawn)
        case r: ScrapeSkillIconsRunner => r.run(client, reporter)
        case _ => throw new RuntimeException("Unexpected runner returned")
      }

      Behaviors.receiveSignal[Nothing] { case (context, _: Terminated) =>
        if (context.children.isEmpty)
          Behaviors.stopped
        else
          Behaviors.same
      }
    },
    name = "scraper"
  )

  Await.result(actorSystem.whenTerminated, atMost = 5.hours)
}
