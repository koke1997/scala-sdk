package controllers.api

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import services.ollama.{GenerationService, ModelService}

import scala.concurrent.ExecutionContext

/**
 * Controller for basic Ollama API interactions
 */
@Singleton
class OllamaController @Inject()(
  cc: ControllerComponents,
  modelService: ModelService,
  generationService: GenerationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(getClass)
  
  /**
   * List available models
   */
  def listModels() = Action.async {
    logger.info("Requesting list of Ollama models")
    modelService.listModels().map(Ok(_))
  }
  
  /**
   * Generate text
   */
  def generateText() = Action(parse.text).async { request =>
    logger.info(s"Generate text request received, length: ${request.body.length}")
    generationService.generateFromText(request.body).map(Ok(_))
  }
  
  /**
   * Debug request details
   */
  def debug() = Action(parse.raw) { request =>
    val bodyText = request.body.asBytes().map(_.utf8String).getOrElse("")
    Ok(Json.obj(
      "contentType" -> Json.toJson(request.contentType.getOrElse("none")),
      "bodyLength" -> Json.toJson(bodyText.length),
      "bodyText" -> Json.toJson(bodyText),
      "headers" -> Json.toJson(request.headers.headers.map(h => h._1 + ": " + h._2).mkString("\n")),
      "timestamp" -> Json.toJson(System.currentTimeMillis())
    ))
  }
}

