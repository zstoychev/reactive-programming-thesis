package http.web_server

import java.util.concurrent.ThreadLocalRandom

import functional.Monad
import future.concurrent.Executors.defaultExecutor
import http.Http.{HttpBody, HttpResponse, StatusCodes}

import scala.concurrent.duration._
import scala.io.Source
import scala.util.control.NonFatal
import StatusCodes._

object WebServerActions {
  val largeResponse = {
    val input = getClass.getResourceAsStream("/functional-programming.html")
    val result = Source.fromInputStream(input, "UTF-8").mkString.getBytes
    input.close()
    val body = HttpBody.Custom(Some("text/html; charset=UTF-8"), result)
    HttpResponse(ok, Map.empty, body)
  }

  val actions = {
    import http.dsl.CallbackBasedHttpDsL._

    get("/") { (ctx,  request) =>
      HttpResponse(ok, Map.empty, HttpBody.Text("Hello World!"))
    } ~
    get("/large-resource") { (ctx, request) =>
      largeResponse
    } ~
    get("/random") { (ctx, request) =>
      HttpResponse(ok, Map.empty, HttpBody.Json(ThreadLocalRandom.current().nextInt()))
    } ~
    getAsync("/random-delayed") { (ctx, request) => handler =>
      ctx.afterTimeout(200.millis) {
        handler(HttpResponse(ok, Map.empty, HttpBody.Json(ThreadLocalRandom.current().nextInt())))
      }
    } ~
    getAsync("/sum-of-three-randoms") { (ctx, request) => handler =>
      def retrieveNumber(response: Option[HttpResponse]) = response map { r => new String(r.body.data).toInt }

      ctx.retrieve("localhost", 8001, "/random") { firstResponse =>
        ctx.retrieve("localhost", 8001, "/random") { secondResponse =>
          ctx.retrieve("localhost", 8001, "/random") { thirdResponse =>
            Monad.sequence(List(firstResponse, secondResponse, thirdResponse) map retrieveNumber) map { _.sum } match {
              case Some(sum) => handler(HttpResponse(ok, Map.empty, HttpBody.Json(sum)))
              case None => handler(HttpResponse(serviceUnavailable, Map.empty, HttpBody.Empty))
            }
          }
        }
      }
    } ~
    getAsync("/sum-of-three-randoms-delayed") { (ctx, request) => handler =>
      def retrieveNumber(response: Option[HttpResponse]) = response map { r => new String(r.body.data).toInt }

      ctx.retrieve("localhost", 8001, "/random-delayed") { firstResponse =>
        ctx.retrieve("localhost", 8001, "/random-delayed") { secondResponse =>
          ctx.retrieve("localhost", 8001, "/random-delayed") { thirdResponse =>
            Monad.sequence(List(firstResponse, secondResponse, thirdResponse) map retrieveNumber) map { _.sum } match {
              case Some(sum) => handler(HttpResponse(ok, Map.empty, HttpBody.Json(sum)))
              case None => handler(HttpResponse(serviceUnavailable, Map.empty, HttpBody.Empty))
            }
          }
        }
      }
    }
  }

  val futureBasedActions = {
    import http.dsl.FutureBasedHttpDsL._

    val serviceUnavailableResponse = HttpResponse(serviceUnavailable, Map.empty, HttpBody.Empty)

    get("/") { (ctx,  request) =>
      HttpResponse(ok, Map.empty, HttpBody.Text("Hello World!"))
    } ~
    get("/large-resource") { (ctx, request) =>
      largeResponse
    } ~
    get("/random") { (ctx, request) =>
      HttpResponse(ok, Map.empty, HttpBody.Json(ThreadLocalRandom.current().nextInt()))
    } ~
    getAsync("/random-delayed") { (ctx, request) =>
      ctx.afterTimeout(40.millis) {
        HttpResponse(ok, Map.empty, HttpBody.Json(ThreadLocalRandom.current().nextInt()))
      }
    } ~
    getAsync("/sum-of-three-randoms") { (ctx, request) =>
          val randoms = List.fill(3)(ctx.retrieve("localhost", 8001, "/").map(r => new String(r.body.data).toInt))

      Monad.sequence(randoms)
        .map { _.sum }
        .map { sum => HttpResponse(ok, Map.empty, HttpBody.Json(sum)) }
        .recover { case NonFatal(e) => serviceUnavailableResponse }
    } ~
    getAsync("/sum-of-three-randoms-sequential") { (ctx, request) =>
      val randomCall = () => ctx.retrieve("localhost", 8001, "/") map { r => new String(r.body.data).toInt }

      val sumFuture = for {
        a <- randomCall()
        b <- randomCall()
        c <- randomCall()
      } yield a + b + c

      sumFuture map { sum => HttpResponse(ok, Map.empty, HttpBody.Json(sum)) } recover {
        case NonFatal(e) => serviceUnavailableResponse
      }
    } ~
    getAsync("/sum-of-three-randoms-parallel-2") { (ctx, request) =>
      val randomCall = () => ctx.retrieve("localhost", 8001, "/") map { r => new String(r.body.data).toInt }
      val List(firstCall, secondCall, thirdCall) = List.fill(3)(randomCall())

      val sumFuture = for {
        a <- firstCall
        b <- secondCall
        c <- thirdCall
      } yield a + b + c

      sumFuture map { sum => HttpResponse(ok, Map.empty, HttpBody.Json(sum)) } recover {
        case NonFatal(e) => serviceUnavailableResponse
      }
    }
  }
}
