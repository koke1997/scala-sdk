package services.ollama

import javax.inject._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient
import core.config.{OllamaConfig, HttpConfig}
import core.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Implementation of HTTP client for Ollama API
 */
@Singleton
class OllamaClient @Inject()(
  ws: WSClient,
  ollamaConfig: OllamaConfig,
  httpConfig: HttpConfig
)(implicit val executionContext: ExecutionContext) extends HttpClient {
  
  private val logger = Logger(getClass)
  
  /**
   * GET request to Ollama API
   */
  def get(url: String): Future[JsValue] = {
    logger.info(s"GET request to $url")
    
    ws.url(url)
      .withRequestTimeout(httpConfig.requestTimeout)  // Use Scala Duration directly
      .get()
      .map(response => response.json)
      .recover {
        case e: Exception =>
          logger.error(s"Request error: ${e.getMessage}", e)
          Json.obj("error" -> s"Request failed: ${e.getMessage}")
      }
  }
  
  /**
   * POST request with JSON body
   */
  def post(url: String, body: JsValue): Future[JsValue] = {
    logger.info(s"POST request to $url")
    logger.debug(s"Request body: ${Json.stringify(body)}")
    
    ws.url(url)
      .withRequestTimeout(httpConfig.requestTimeout)  // Use Scala Duration directly
      .post(body)
      .map(response => response.json)
      .recover {
        case e: Exception =>
          logger.error(s"Request error: ${e.getMessage}", e)
          Json.obj("error" -> s"Request failed: ${e.getMessage}")
      }
  }
  
  /**
   * POST request with text body
   */
  def postText(url: String, body: String): Future[JsValue] = {
    logger.info(s"POST text request to $url")
    
    val jsonBody = Json.obj(
      "model" -> ollamaConfig.defaultModel,
      "prompt" -> body
    )
    
    post(url, jsonBody)
  }
  
  /**
   * POST request for streaming
   */
  def postStream(url: String, body: JsValue): Future[JsValue] = {
    // For non-streaming implementations, just use regular post
    post(url, body)
  }
}