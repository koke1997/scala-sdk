package services

import javax.inject._
import play.api.mvc.{Request, Result, Results}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for handling authentication and authorization.
 */
@Singleton
class AuthService @Inject()(
  implicit ec: ExecutionContext
) {
  // In a real application, you would implement proper token validation
  private val validTokens = Set("test-token-1", "test-token-2")

  /**
   * Validates the API token in the request header.
   */
  def validateToken(request: Request[_]): Boolean = {
    request.headers.get("X-API-Token").exists(validTokens.contains)
  }

  /**
   * Authentication action filter.
   */
  def withAuth(block: => Future[Result])(implicit request: Request[_]): Future[Result] = {
    if (validateToken(request)) {
      block
    } else {
      Future.successful(Results.Unauthorized(Json.obj("error" -> "Invalid or missing API token")))
    }
  }
}
