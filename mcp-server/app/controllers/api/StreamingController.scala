package controllers.api

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import org.apache.pekko.util.ByteString
import services.streaming.StreamingService

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
 * Controller for streaming responses from Ollama
 */
@Singleton
class StreamingController @Inject()(
  cc: ControllerComponents,
  streamingService: services.streaming.StreamingService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(getClass)
  
  /**
   * Stream generation from JSON request
   */
  def streamGenerate() = Action(parse.raw) { request =>
    logger.info("Stream generation request received")
    
    val bytes = request.body.asBytes().getOrElse(ByteString.empty)
    
    if (bytes.isEmpty) {
      BadRequest(Json.obj("error" -> "Empty request body"))
    } else {
      val bodyString = bytes.utf8String
      
      val requestBodyJson = Try(Json.parse(bodyString)).getOrElse {
        Json.obj("prompt" -> bodyString.take(500))
      }
      
      val source = streamingService.streamGeneration(requestBodyJson)
      Ok.chunked(source).as("application/json")
    }
  }
  
  /**
   * Stream generation from plain text
   */
  def streamGenerateText() = Action(parse.text) { request =>
    logger.info("Stream text generation request received")
    
    val source = streamingService.streamGenerationFromText(request.body)
    Ok.chunked(source).as("application/json")
  }
}

