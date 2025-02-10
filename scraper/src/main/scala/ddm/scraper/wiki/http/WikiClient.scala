package ddm.scraper.wiki.http


import cats.data.NonEmptyList
import ddm.common.utils.circe.JsonObjectOps.{decodeNestedField, decodeOptField}
import ddm.scraper.http.HTTPClient
import ddm.scraper.telemetry.{WithAnnotation, WithStreamAnnotation}
import ddm.scraper.wiki.http.response.WikiResponse
import ddm.scraper.wiki.model.{Page, PageDescriptor}
import ddm.scraper.wiki.streaming.{PageStream, pageFlatMapPar, pageFlattenIterables, pageMapZIO}
import io.circe.{CursorOp, Decoder, JsonObject, parser}
import zio.http.*
import zio.http.Header.UserAgent
import zio.stream.ZStream
import zio.{Chunk, Schedule, Task, Trace, ZIO}

import java.net.URLEncoder
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
      // It's a bit weird to duplicate the page like this, but it lets us use
      // all the utils for page mapping
      .map(_.map((page, _) => (page, page)))

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
      val encodedFileName = URLEncoder.encode(
        s"${fileName.raw}.${fileName.extension}".replace(' ', '_'),
        StandardCharsets.UTF_8
      )
      val request = buildRequest(baseURL.addPath(s"/images/$encodedFileName"))

      execute(request, kindLabel = "image-download")
        .catchSome { case WikiFetchException.HTTPError(Status.NotFound, _) =>
          lookupImageURL(encodedFileName).flatMap(actualURL =>
            execute(buildRequest(actualURL), kindLabel = "redirected-image-download")
          )
        }
    }

  private def lookupImageURL(encodedFileName: String)(using Trace): Task[URL] =
    execute(
      buildRequest(baseURL.addPath(s"/rest.php/v1/file/$encodedFileName")),
      kindLabel = "lookup-image-url"
    ).flatMap(metadata => ZIO.fromEither(
      for {
        json <- parser.decode[JsonObject](String(metadata, StandardCharsets.UTF_8))
        rawActualURL <- json.decodeNestedField[String]("preferred", "url")(List.empty)
        actualURL <- URL.decode(rawActualURL)
      } yield actualURL
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

  private def fetchPages(baseParams: QueryParams)(using Trace): PageStream[JsonObject] = {
    val request = buildQuery(baseParams)
    val response = execute(request, kindLabel = "query").either.map(_.flatMap(decodeQueryResponse))

    ZStream
      .fromZIO(response)
      .flatMap {
        case Left(error) =>
          ZStream.succeed(Left((request, error)))

        case Right(response) =>
          val continue = response.continueParams match {
            case Some(continueParams) => fetchPages(baseParams ++ continueParams)
            case None => ZStream.empty
          }

          ZStream
            .fromIterable(response.pages)
            .map(page => Right(page))
            .concat(continue)
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

  private def decodeQueryResponse(response: Array[Byte]): Either[Exception, WikiResponse.Success] =
    parser
      .decode[WikiResponse](String(response, StandardCharsets.UTF_8))
      .flatMap {
        case f: WikiResponse.Failure => Left(WikiFetchException.ErrorResponse(f))
        case s: WikiResponse.Success => Right(s)
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
