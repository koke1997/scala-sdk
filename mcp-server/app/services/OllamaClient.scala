package services

import javax.inject._
import play.api.Configuration
import play.api.libs.ws._
import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}
import play.api.Logger

/**
 * Service to interact with the Ollama API
 */
@Singleton
class OllamaClient @Inject()(
  ws: WSClient,
  config: Configuration
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(this.getClass)
  private val baseUrl = config.get[String]("ollama.baseUrl")
  
  /**
   * List available models
   */
  def listModels(): Future[JsValue] = {
    logger.debug(s"Calling Ollama API to list models: $baseUrl/tags")
    
    ws.url(s"$baseUrl/tags")
      .get()
      .map { response =>
        logger.debug(s"Ollama API response (models): ${response.body}")
        response.json
      }
      .recover {
        case e: Exception =>
          logger.error(s"Error listing models: ${e.getMessage}", e)
          Json.obj("error" -> e.getMessage)
      }
  }
  
  /**
   * Generate text with a model
   */
  def generate(request: JsValue): Future[JsValue] = {
    logger.debug(s"Calling Ollama API to generate text: $baseUrl/generate")
    logger.debug(s"Request body: ${Json.stringify(request)}")
    
    ws.url(s"$baseUrl/generate")
      .post(request)
      .map { response =>
        logger.debug(s"Ollama API response (generate): ${response.body}")
        response.json
      }
      .recover {
        case e: Exception =>
          logger.error(s"Error generating text: ${e.getMessage}", e)
          Json.obj("error" -> e.getMessage)
      }
  }
}

