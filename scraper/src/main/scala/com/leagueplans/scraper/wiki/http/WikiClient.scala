package com.leagueplans.scraper.wiki.http

import cats.data.NonEmptyList
import com.leagueplans.common.utils.circe.JsonObjectOps.{decodeNestedField, decodeOptField}
import com.leagueplans.scraper.http.HTTPClient
import com.leagueplans.scraper.telemetry.{WithAnnotation, WithStreamAnnotation}
import com.leagueplans.scraper.wiki.http.response.{FilePage, WikiResponse}
import com.leagueplans.scraper.wiki.model.{FileInfo, PageDescriptor}
import com.leagueplans.scraper.wiki.streaming.*
import io.circe.{CursorOp, Decoder, JsonObject, parser}
import zio.http.*
import zio.http.Header.UserAgent
import zio.stream.ZStream
import zio.{Chunk, Schedule, Task, Trace, ZIO}

import java.nio.charset.StandardCharsets

private object WikiClient {
  private val retryableStatuses: Set[Status] = Set(
    Status.RequestTimeout,
    Status.InternalServerError,
    Status.BadGateway,
    Status.ServiceUnavailable,
    Status.GatewayTimeout,
  )

  private val isRetryableHTTPError: Schedule[Any, Response, Response] =
    Schedule.recurWhile[Response](response =>
      retryableStatuses.contains(response.status)
    )
    
  private val streamFanOut = 4
}

final class WikiClient(
  httpClient: HTTPClient,
  userAgent: UserAgent,
  baseURL: URL,
  pageLimit: Int,
  retrySchedule: Schedule[Any, Unit, ?]
) {
  private val apiURL: URL = baseURL.addPath("/api.php")

  def fetch(selector: WikiSelector)(using Trace): PageStream[PageDescriptor] =
    ZStream
      .fromIterable(QueryParamsGenerator(selector, pageLimit))
      .flatMapPar(WikiClient.streamFanOut)(fetchPages)
      .pageExtend
      .pageMap((page, _) => page)

  def fetch(selector: WikiSelector, contentType: WikiContentType)(using Trace): PageStream[String] = {
    val contentTypeParams = QueryParamsGenerator(contentType)
    ZStream
      .fromIterable(QueryParamsGenerator(selector, pageLimit))
      .map(_ ++ contentTypeParams)
      .flatMapPar(WikiClient.streamFanOut)(fetchPages)
      .pageMapZIO(json => decodeContent(json, contentType) match {
        case Some(Right(content)) => ZIO.succeed(Chunk(content))
        case Some(Left(error)) => ZIO.fail(error)
        case None => ZIO.logDebug("Ignoring page with no content").as(Chunk.empty)
      })
      .pageFlattenIterables
  }

  def fetchImage(fileName: PageDescriptor.Name.File)(using Trace): Task[Array[Byte]] =
    WithAnnotation.forLogs("wiki-image-name" -> s"${fileName.raw}.${fileName.extension}") {
      // Left unencoded: zio-http (3.11) encodes a path as it sends it, so a name encoded
      // here goes out encoded twice (`(` as `%2528`) and the wiki 404s.
      val pathFileName = s"${fileName.raw}.${fileName.extension}".replace(' ', '_')
      val request = buildRequest(baseURL.addPath(s"/images/$pathFileName"))

      execute(request, kindLabel = "image-download")
        .catchSome { case WikiFetchException.HTTPError(Status.NotFound, _) =>
          lookupImageURL(pathFileName).flatMap(actualURL =>
            execute(buildRequest(actualURL), kindLabel = "redirected-image-download")
          )
        }
    }

  /** What the wiki reports about each file, without downloading any of them. A file the
    * wiki reports nothing for is left out, so its caller can fall back on downloading it.
    */
  def fetchFileInfo(
    files: Iterable[PageDescriptor.Name.File]
  )(using Trace): Task[Map[PageDescriptor.Name.File, FileInfo]] =
    ZIO
      .foreach(files.toVector.distinct.grouped(pageLimit).toVector)(batch =>
        fetchResponses(QueryParamsGenerator.fileInfo(batch), Decoder[FilePage], kindLabel = "file-info")
          .runCollect
          .flatMap(results => ZIO.fromEither(toFileInfo(batch, results)))
      )
      .map(_.flatten.toMap)

  /** Matches each file asked about with the page the wiki answered under, which is its own
    * spelling of the name, or the file a redirect points to. A continued query can spread a
    * file's details over several responses, so they are read together.
    */
  private def toFileInfo(
    files: Vector[PageDescriptor.Name.File],
    results: Chunk[Either[PageStream.Error, WikiResponse.Success[FilePage]]]
  ): Either[Throwable, Vector[(PageDescriptor.Name.File, FileInfo)]] =
    results.collectFirst { case Left((_, error)) => error } match {
      case Some(error) =>
        Left(error)

      case None =>
        val responses = results.collect { case Right(response) => response }
        val sha1s = responses.flatMap(_.pages).collect { case FilePage(title, Some(sha1)) => title -> sha1 }.toMap

        Right(files.flatMap(file =>
          responses
            .iterator
            .flatMap(response => sha1s.get(response.resolve(file.wikiName)))
            .nextOption()
            .map(sha1 => file -> FileInfo(sha1))
        ))
    }

  private def lookupImageURL(pathFileName: String)(using Trace): Task[URL] =
    execute(
      buildRequest(baseURL.addPath(s"/rest.php/v1/file/$pathFileName")),
      kindLabel = "lookup-image-url"
    ).flatMap(metadata => ZIO.fromEither(
      for {
        json <- parser.decode[JsonObject](String(metadata, StandardCharsets.UTF_8))
        rawActualURL <- json.decodeNestedField[String]("preferred", "url")(List.empty)
        actualURL <- URL.decode(rawActualURL)
        // The wiki appends a cache-busting query (`?fa8bd`), which has to go. When a URL
        // has a query, zio-http (3.8.0) percent-encodes its path again as it sends it, so
        // `%28` goes out as `%2528` and the wiki 404s; without one, the path is sent as
        // given. Our paths arrive already encoded, so they only survive without a query.
        // The file is served the same either way.
      } yield actualURL.copy(queryParams = QueryParams.empty)
    ))

  def fetchAllMembers(category: PageDescriptor.Name.Category)(using Trace): PageStream[PageDescriptor] =
    WithStreamAnnotation.forLogs("wiki-category" -> category.raw)(
      fetch(WikiSelector.Members(category)).pageFlatMapPar(WikiClient.streamFanOut) {
        case PageDescriptor(_, subCategory: PageDescriptor.Name.Category) =>
          fetchAllMembers(subCategory)
        case other =>
          ZStream.succeed(Right((other, other)))
      }
    )

  private def fetchPages(baseParams: QueryParams)(using Trace): PageStream[JsonObject] =
    fetchResponses(baseParams, WikiResponse.existingPage, kindLabel = "query").flatMap {
      case Left(error) => ZStream.succeed(Left(error))
      case Right(response) => ZStream.fromIterable(response.pages).map(page => Right(page))
    }

  /** Every response to a query, following its continuations until the wiki has no more. */
  private def fetchResponses[P](
    baseParams: QueryParams,
    page: Decoder[P],
    kindLabel: String,
    continueParams: QueryParams = QueryParams.empty
  )(using Trace): ZStream[Any, Nothing, Either[PageStream.Error, WikiResponse.Success[P]]] = {
    val request = buildQuery(baseParams ++ continueParams)
    val response = execute(request, kindLabel).either.map(_.flatMap(decodeQueryResponse(_, page)))

    ZStream
      .fromZIO(response)
      .flatMap {
        case Left(error) =>
          ZStream.succeed(Left((request, error)))

        case Right(response) =>
          val continue = response.continueParams match {
            case Some(nextContinueParams) => fetchResponses(baseParams, page, kindLabel, nextContinueParams)
            case None => ZStream.empty
          }

          ZStream.succeed(Right(response)).concat(continue)
      }
  }

  private def execute(request: Request, kindLabel: String)(using Trace): Task[Array[Byte]] =
    httpClient
      .execute(
        request,
        kindLabel,
        retrySchedule.contramap[Any, Response](_ => ()) && WikiClient.isRetryableHTTPError
      )
      .flatMap(response => response.body.asArray.map((response.status, _)))
      .flatMap {
        case (status, data) if status.isSuccess =>
          ZIO.succeed(data)
        case (status, data) =>
          val body = String(data, StandardCharsets.UTF_8)
          ZIO.fail(WikiFetchException.HTTPError(status, body))
      }

  private def buildQuery(params: QueryParams): Request =
    buildRequest(apiURL) ++ QueryParamsGenerator.query ++ params

  private def buildRequest(url: URL): Request =
    Request.get(url).addHeader(userAgent)

  private def decodeQueryResponse[P](
    response: Array[Byte],
    page: Decoder[P]
  ): Either[Exception, WikiResponse.Success[P]] =
    parser
      .decode[WikiResponse[P]](String(response, StandardCharsets.UTF_8))(using WikiResponse.decoder(using page))
      .flatMap {
        case f: WikiResponse.Failure => Left(WikiFetchException.ErrorResponse(f))
        case s: WikiResponse.Success[P] => Right(s)
      }

  private def decodeContent(
    json: JsonObject,
    contentType: WikiContentType,
  ): Option[Decoder.Result[String]] =
    json.decodeOptField[NonEmptyList[JsonObject]](contentType.prop) match {
      case Left(error) => Some(Left(error))
      case Right(None) => None
      case Right(Some(data)) =>
        Some(decodeContent(data.head, contentType, CursorOp.Field(contentType.prop)))
    }

  private def decodeContent(
    json: JsonObject,
    contentType: WikiContentType,
    cursorOp: CursorOp
  ): Decoder.Result[String] =
    contentType match {
      case WikiContentType.Revisions =>
        json.decodeNestedField[String]("slots", "main", "content")(List(cursorOp))
    }
}
