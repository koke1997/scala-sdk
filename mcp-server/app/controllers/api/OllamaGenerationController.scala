package controllers.api

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import org.apache.pekko.util.ByteString
import services.ollama.GenerationService

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Controller for Ollama text generation
 */
@Singleton
class OllamaGenerationController @Inject()(
  cc: ControllerComponents,
  generationService: services.ollama.GenerationService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(getClass)
  
  /**
   * Generate text from JSON request
   */
  def generate() = Action(parse.raw).async { request =>
    logger.info("Generate text request received")
    
    val bytes = request.body.asBytes().getOrElse(ByteString.empty)
    
    if (bytes.isEmpty) {
      Future.successful(BadRequest(Json.obj("error" -> "Empty request body")))
    } else {
      val bodyString = bytes.utf8String
      
      val requestBodyJson = Try(Json.parse(bodyString)).getOrElse {
        Json.obj("prompt" -> bodyString.take(500))
      }
      
      generationService.generate(requestBodyJson).map(Ok(_))
    }
  }
}