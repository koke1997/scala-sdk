package services.ollama

import javax.inject._
import play.api.Logger
import play.api.libs.json._
import core.config.OllamaConfig

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for text generation with Ollama
 */
@Singleton
class GenerationService @Inject()(
  client: OllamaClient,
  config: OllamaConfig
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  
  /**
   * Generate text from JSON request
   */
  def generate(request: JsValue): Future[JsValue] = {
    logger.info("Generating text from JSON request")
    val requestWithModel = ensureModel(request)
    client.post(s"${config.baseUrl}/generate", requestWithModel)
  }
  
  /**
   * Generate text from plain text prompt
   */
  def generateFromText(text: String): Future[JsValue] = {
    logger.info(s"Generating text from plain text, length: ${text.length}")
    val jsonBody = Json.obj(
      "model" -> config.defaultModel,
      "prompt" -> text
    )
    generate(jsonBody)
  }
  
  /**
   * Ensure the request has a model specified
   */
  private def ensureModel(request: JsValue): JsValue = {
    if ((request \ "model").isDefined) {
      request
    } else {
      request.as[JsObject] + ("model" -> JsString(config.defaultModel))
    }
  }
}

