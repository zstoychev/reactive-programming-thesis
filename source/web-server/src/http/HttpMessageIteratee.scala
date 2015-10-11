package http

import http.Http._
import iteratee.simple._
import iteratee.{EOF, El, Empty, Input}
import parser.ParseException

object HttpMessageIteratee {
  private val RequestLineFormat = "^(\\S+) (\\S+) HTTP/1.1$".r
  private val StatusLineFormat = "^HTTP/1.1 (\\d* .*)$".r
  private val HeaderFormat = "(^[^:]*): (.*)$".r

  val requestLineIteratee = Iteratee.asciiLineChunked flatMap {
    case RequestLineFormat(method, path) => Done((method, path), Empty)
    case _ => Error(new ParseException("Invalid request line"))
  }

  val statusLineIteratee = Iteratee.asciiLineChunked flatMap {
    case StatusLineFormat(statusCode) => Done(statusCode, Empty)
    case _ => Error(new ParseException("Invalid status line"))
  }

  val headersIteratee: Iteratee[String, HttpHeaders] = {
    def step(headers: HttpHeaders): Input[String] => Iteratee[String, HttpHeaders] = {
      case El("") => Done(headers, Empty)
      case El(HeaderFormat(key, value)) =>
        Cont(step(headers + (key -> value)))
      case El(_) => Error(new ParseException("Invalid header"))
    }
    Cont(step(Map.empty))
  }

  def bytesIteratee(bytesCount: Int): Iteratee[Array[Byte], Array[Byte]] = {
    def step(body: Array[Byte]): Input[Array[Byte]] => Iteratee[Array[Byte], Array[Byte]] = {
      case El(bytes) if body.length + bytes.length < bytesCount =>
        Cont(step(Array.concat(bytes, body)))
      case El(bytes) =>
        val (last, rest) = bytes.splitAt(bytesCount - body.length)
        Done(Array.concat(body, last), El(rest))
      case Empty => Cont(step(body))
      case EOF => Error(new ParseException("EOF before reading whole body"))

    }
    Cont(step(Array.empty))
  }

  def bodyIteratee(headers: HttpHeaders): Iteratee[Array[Byte],HttpBody] = {
    headers.get(Headers.ContentLengthHeader)
      .map { _.toInt }
      .map { bytesIteratee(_) map { bytes =>
        HttpBody.Custom(headers.get(Headers.ContentTypeHeader), bytes) }
      }
      .getOrElse(Done(HttpBody.Empty, Empty))
  }

  val httpRequestIteratee = for {
    (method, path) <- requestLineIteratee
    headers <- Enumeratee.asciiLines transform headersIteratee
    body <- bodyIteratee(headers)
  } yield HttpRequest(method, path, headers, body)

  val httpResponseIteratee = for {
    statusCode <- statusLineIteratee
    headers <- Enumeratee.asciiLines transform headersIteratee
    body <- bodyIteratee(headers)
  } yield HttpResponse(statusCode, headers, body)
}
