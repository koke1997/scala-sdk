package controllers

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Base controller that provides common functionality for all controllers.
 */
abstract class MCPBaseController(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  /**
   * Helper method to handle JSON requests.
   */
  def withJsonBody[A](f: A => Future[Result])(implicit request: Request[JsValue], reads: Reads[A]): Future[Result] = {
    request.body.validate[A].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      },
      model => f(model)
    )
  }
  
  /**
   * Helper method for returning standardized error responses.
   */
  def errorResponse(message: String, status: Int = 400): Result = {
    Status(status)(Json.obj("error" -> message))
  }
}
