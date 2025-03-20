package controllers

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import services.PekkoOllamaClient
import org.apache.pekko.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

@Singleton
class PekkoOllamaController @Inject()(
  cc: ControllerComponents,
  ollamaClient: PekkoOllamaClient
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(this.getClass)
  
  // List available models
  def listModels() = Action.async {
    logger.info("Requesting list of Ollama models via Pekko HTTP")
    
    ollamaClient.listModels()
      .map(Ok(_))
      .recover {
        case e: Exception =>
          logger.error(s"Error listing models: ${e.getMessage}", e)
          InternalServerError(Json.obj("error" -> s"Failed to fetch models: ${e.getMessage}"))
      }
  }
  
  // Generate text - simple approach that works around body parsing issues
  def generate() = Action(parse.raw).async { request =>
    logger.info("Generate text request received via Pekko endpoint")
    
    // Get content as bytes and convert to string
    val bytes = request.body.asBytes().getOrElse(ByteString.empty)
    val contentType = request.contentType.getOrElse("unknown")
    
    logger.debug(s"Request content type: $contentType")
    logger.debug(s"Request body size: ${bytes.size} bytes")
    
    if (bytes.isEmpty) {
      logger.warn("Empty request body")
      Future.successful(BadRequest(Json.obj("error" -> "Empty request body")))
    } else {
      val bodyString = bytes.utf8String
      logger.debug(s"Request body: $bodyString")
      
      Try(Json.parse(bodyString)).fold(
        error => {
          // If parsing fails, try to create a default request
          logger.warn(s"Failed to parse request as JSON: ${error.getMessage}")
          
          // Use default values if parsing failed
          val defaultJson = Json.obj(
            "model" -> "phi4-mini", 
            "prompt" -> bodyString.take(500) // Use the raw text as prompt if JSON parsing fails
          )
          
          ollamaClient.generate(defaultJson).map(Ok(_))
        },
        json => {
          // Successfully parsed as JSON
          logger.debug(s"Parsed JSON: $json")
          ollamaClient.generate(json).map(Ok(_))
        }
      )
    }
  }
  
  // Simple text endpoint - accepts plain text and wraps in JSON
  def generateText() = Action(parse.text).async { request =>
    logger.info("Generate text (plain) request received via Pekko endpoint")
    logger.debug(s"Text body: ${request.body}")
    
    val jsonBody = Json.obj(
      "model" -> "phi4-mini", 
      "prompt" -> request.body
    )
    
    ollamaClient.generate(jsonBody).map(Ok(_))
  }
}

