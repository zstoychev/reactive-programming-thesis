package http.dsl

import future.concurrent.Future
import http.Http.StatusCodes._
import http.Http._
import http.dsl.HttpDSL._
import http.dsl.context.callback.CallbackBasedContext
import http.dsl.context.future.FutureBasedContext

trait HttpDSL[CTX, AR] {
  import HttpMethods._

  def buildAsyncActionResult(httpResult: HttpResponse): AR
  def buildAsyncAction(action: Action[CTX]): AsyncAction[CTX, AR] =
    (ctx, request) => buildAsyncActionResult(action(ctx, request))

  val get = custom(GET) _
  val post = custom(POST) _
  val put = custom(PUT) _
  val patch = custom(PATCH) _
  val delete = custom(DELETE) _

  val getAsync = customAsync(GET) _
  val postAsync = customAsync(POST) _
  val putAsync = customAsync(PUT) _
  val patchAsync = customAsync(PATCH) _
  val deleteAsync = customAsync(DELETE) _

  def custom(method: String)(path: String)(action: Action[CTX]) = HttpActions(this)(
    Map((method, path) -> buildAsyncAction(action)))
  def customAsync(method: String)(path: String)(action: AsyncAction[CTX, AR]) =
    HttpActions(this)(Map((method, path) -> action))
}

object HttpDSL {
  type Action[CTX] = (CTX, HttpRequest) => HttpResponse
  type AsyncAction[CTX, AR] = (CTX, HttpRequest) => AR
}

case class HttpActions[CTX, AR](httpDSL: HttpDSL[CTX, AR])
                                   (val actions: Map[(String, String), AsyncAction[CTX, AR]]) {
  def ~(other: HttpActions[CTX, AR]) = HttpActions(httpDSL)(actions ++ other.actions)

  def execute(httpRequest: HttpRequest)(implicit ctx: CTX): AR = httpRequest match {
    case HttpRequest(method, path, headers, body) =>
      actions.get((method, path)) map { _(ctx, httpRequest) } getOrElse {
        httpDSL.buildAsyncActionResult(HttpResponse(notFound, Map.empty, HttpBody.Empty))
      }
  }
}

object CallbackBasedHttpDsL extends HttpDSL[CallbackBasedContext, (HttpResponse => Unit) => Unit] {
  type AsyncActionResult = (HttpResponse => Unit) => Unit

  def buildAsyncActionResult(httpResult: HttpResponse): AsyncActionResult = f => f(httpResult)
}

object FutureBasedHttpDsL extends HttpDSL[FutureBasedContext, Future[HttpResponse]] {
  type AsyncActionResult = Future[HttpResponse]

  def buildAsyncActionResult(httpResult: HttpResponse): AsyncActionResult = Future.successful(httpResult)
}
