package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import services.RequestLogger

/**
 * Home controller with basic status endpoints and dashboard.
 */
@Singleton
class HomeController @Inject()(
  cc: ControllerComponents,
  implicit val assetsFinder: AssetsFinder
) extends AbstractController(cc) {

  /**
   * Root endpoint that returns the index page.
   */
def index(title: String) = Action { implicit request: Request[AnyContent] =>
  Ok(views.html.index(title))
}
  
  /**
   * Dashboard page with more detailed information and logging.
   */
  def dashboard() = Action { implicit request: Request[AnyContent] =>
    // Use the dashboard template
    Ok(views.html.dashboard("MCP Server Dashboard"))
  }
  
  /**
   * Health check endpoint that can be used for monitoring.
   * Returns basic health information about the service.
   */
  def healthCheck() = Action { implicit request: Request[AnyContent] =>
    val currentTime = System.currentTimeMillis()
    val uptime = java.lang.management.ManagementFactory.getRuntimeMXBean.getUptime()
    
    Ok(Json.obj(
      "status" -> JsString("healthy"),
      "timestamp" -> JsNumber(currentTime),
      "uptime_ms" -> JsNumber(uptime),
      "environment" -> JsString(sys.env.getOrElse("APP_ENV", "development")),
      "version" -> JsString("1.0.0")
    ))
  }
  
  /**
   * API endpoint to get server status as JSON for the dashboard.
   */
  def status() = Action { implicit request: Request[AnyContent] =>
    // Get recent requests from the RequestLogger
    val recentRequests = RequestLogger.getRecentRequests()
    
    Ok(Json.obj(
      "status" -> JsString("ok"),
      "message" -> JsString("Model Context Protocol (MCP) Server is running"),
      "version" -> JsString("1.0.0"),
      "requests" -> recentRequests
    ))
  }
  
  /**
   * Readiness probe that can be used in containerized environments.
   */
  def ready() = Action { implicit request: Request[AnyContent] =>
    Ok(Json.obj(
      "status" -> JsString("ready"),
      "ready" -> JsBoolean(true),
      "timestamp" -> JsNumber(System.currentTimeMillis())
    ))
  }
}