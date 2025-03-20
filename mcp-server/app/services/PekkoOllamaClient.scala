package services

import javax.inject._
import play.api.{Configuration, Logger}
import play.api.libs.json._
import play.api.libs.ws.WSClient

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Try, Success, Failure}

/**
 * Simplified client for Ollama using Play's WS client instead of Pekko HTTP
 */
@Singleton
class PekkoOllamaClient @Inject()(
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(this.getClass)
  private val ollamaBaseUrl = config.get[String]("ollama.baseUrl")
  
  /**
   * List available models
   */
  def listModels(): Future[JsValue] = {
    val url = s"$ollamaBaseUrl/tags"
    logger.info(s"Sending request to list models: $url")
    
    ws.url(url)
      .get()
      .map { response =>
        logger.debug(s"Response status: ${response.status}")
        logger.debug(s"Response body: ${response.body}")
        
        Try(Json.parse(response.body)) match {
          case Success(json) => json
          case Failure(e) => 
            logger.error(s"Failed to parse response as JSON: ${e.getMessage}")
            Json.obj("error" -> s"Invalid JSON response: ${e.getMessage}")
        }
      }
      .recover {
        case e: Exception =>
          logger.error(s"Error listing models: ${e.getMessage}", e)
          Json.obj("error" -> s"Request failed: ${e.getMessage}")
      }
  }
  
  /**
   * Generate text with a model
   */
  def generate(json: JsValue): Future[JsValue] = {
    val url = s"$ollamaBaseUrl/generate"
    logger.info(s"Sending request to generate text: $url")
    logger.debug(s"Request body: ${Json.stringify(json)}")
    
    ws.url(url)
      .post(json)
      .map { response =>
        logger.debug(s"Response status: ${response.status}")
        logger.debug(s"Response body: ${response.body}")
        
        Try(Json.parse(response.body)) match {
          case Success(json) => json
          case Failure(e) => 
            logger.error(s"Failed to parse response as JSON: ${e.getMessage}")
            Json.obj("error" -> s"Invalid JSON response: ${e.getMessage}")
        }
      }
      .recover {
        case e: Exception =>
          logger.error(s"Error generating text: ${e.getMessage}", e)
          Json.obj("error" -> s"Request failed: ${e.getMessage}")
      }
  }
}

