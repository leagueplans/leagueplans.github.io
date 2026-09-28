package com.leagueplans.scraper.wiki.http

import cats.data.NonEmptyList
import com.leagueplans.common.utils.circe.JsonObjectOps.{decodeNestedField, decodeOptField}
import com.leagueplans.scraper.http.HTTPClient
import com.leagueplans.scraper.telemetry.{WithAnnotation, WithStreamAnnotation}
import com.leagueplans.scraper.wiki.http.response.WikiResponse
import com.leagueplans.scraper.wiki.model.{FileInfo, PageDescriptor}
import com.leagueplans.scraper.wiki.streaming.*
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{CursorOp, Decoder, JsonObject, parser}
import zio.http.*
import zio.http.Header.UserAgent
import zio.stream.ZStream
import zio.{Chunk, Schedule, Task, Trace, ZIO}

import java.nio.charset.StandardCharsets
import java.time.Instant

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

  // The shape of an imageinfo query's response, which unlike the other queries can
  // describe pages that don't exist and so carry no page ID.
  private final case class TitleMapping(from: String, to: String)
  private final case class ImageInfo(sha1: String, timestamp: Instant)
  private final case class FilePage(title: String, imageinfo: Option[List[ImageInfo]])
  private final case class FileQuery(
    normalized: Option[List[TitleMapping]],
    redirects: Option[List[TitleMapping]],
    pages: List[FilePage]
  )

  private given Decoder[TitleMapping] = deriveDecoder
  private given Decoder[ImageInfo] = deriveDecoder
  private given Decoder[FilePage] = deriveDecoder
  private given Decoder[FileQuery] = deriveDecoder
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
      .flatMapPar(WikiClient.streamFanOut)(fetchPages(_, continueParams = QueryParams.empty))
      .pageExtend
      .pageMap((page, _) => page)

  def fetch(selector: WikiSelector, contentType: WikiContentType)(using Trace): PageStream[String] = {
    val contentTypeParams = QueryParamsGenerator(contentType)
    ZStream
      .fromIterable(QueryParamsGenerator(selector, pageLimit))
      .map(_ ++ contentTypeParams)
      .flatMapPar(WikiClient.streamFanOut)(fetchPages(_, continueParams = QueryParams.empty))
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
        execute(buildQuery(QueryParamsGenerator.fileInfo(batch)), kindLabel = "file-info")
          .flatMap(response => ZIO.fromEither(decodeFileInfo(batch, response)))
      )
      .map(_.flatten.toMap)

  private def decodeFileInfo(
    files: Vector[PageDescriptor.Name.File],
    response: Array[Byte]
  ): Either[Exception, Vector[(PageDescriptor.Name.File, FileInfo)]] = {
    val text = String(response, StandardCharsets.UTF_8)

    parser.decode[WikiResponse.Failure](text) match {
      case Right(failure) =>
        Left(WikiFetchException.ErrorResponse(failure))

      case Left(_) =>
        parser
          .decode[JsonObject](text)
          .flatMap(_.decodeNestedField[WikiClient.FileQuery]("query")(List.empty))
          .map { query =>
            // The wiki answers under its own spelling of each name, and under the name of
            // the file a redirect points to, so both have to be followed back.
            val normalised = query.normalized.toList.flatten.map(m => m.from -> m.to).toMap
            val redirects = query.redirects.toList.flatten.map(m => m.from -> m.to).toMap
            val pages = query.pages.map(page => page.title -> page).toMap

            files.flatMap { file =>
              val title = normalised.getOrElse(file.wikiName, file.wikiName)
              val target = redirects.getOrElse(title, title)
              pages
                .get(target)
                .flatMap(_.imageinfo.flatMap(_.headOption))
                .map(info => file -> FileInfo(info.sha1, info.timestamp, viaRedirect = target != title))
            }
          }
    }
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

  private def fetchPages(
    baseParams: QueryParams, 
    continueParams: QueryParams
  )(using Trace): PageStream[JsonObject] = {
    val request = buildQuery(baseParams ++ continueParams)
    val response = execute(request, kindLabel = "query").either.map(_.flatMap(decodeQueryResponse))

    ZStream
      .fromZIO(response)
      .flatMap {
        case Left(error) =>
          ZStream.succeed(Left((request, error)))

        case Right(response) =>
          val continue = response.continueParams match {
            case Some(nextContinueParams) => fetchPages(baseParams, nextContinueParams)
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
      case WikiContentType.LastEdited =>
        json.decodeNestedField[String]("timestamp")(List(cursorOp))
    }
}
