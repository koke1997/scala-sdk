package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import play.api.Logger
import org.apache.pekko.util.ByteString
import play.api.mvc.Results
import scala.concurrent.Future
import scala.concurrent.ExecutionContext

@Singleton
class DebugController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(this.getClass)
  
  // Debug raw body as text
  def debugRaw = Action(parse.raw) { request =>
    val bodyAsBytes = request.body.asBytes().getOrElse(ByteString.empty)
    val bodyAsString = bodyAsBytes.utf8String
    
    logger.debug(s"Raw request received - Content-Type: ${request.contentType}")
    logger.debug(s"Raw body as string: '$bodyAsString'")
    logger.debug(s"Body length: ${bodyAsBytes.length} bytes")
    logger.debug(s"Headers: ${request.headers}")
    
    Ok(Json.obj(
      "contentType" -> Json.toJson(request.contentType.getOrElse("none")),
      "bodyLength" -> Json.toJson(bodyAsBytes.length),
      "bodyString" -> Json.toJson(bodyAsString),
      "headers" -> Json.toJson(request.headers.headers.map(h => h._1 + ": " + h._2).mkString("\n"))
    ))
  }
  
  // Ultra-tolerant JSON handling
  def debugJson = Action(parse.anyContent) { request =>
    logger.debug(s"Request content type: ${request.contentType}")
    
    val jsonBody = request.body.asJson.orElse {
      // Try to parse as text if JSON parsing fails
      request.body.asText.flatMap { text => 
        try {
          Some(Json.parse(text))
        } catch {
          case _: Exception => None
        }
      }
    }.orElse {
      // Try to parse as raw bytes if text parsing fails
      request.body.asRaw.flatMap { raw =>
        try {
          Some(Json.parse(raw.asBytes().getOrElse(ByteString.empty).utf8String))
        } catch {
          case _: Exception => None
        }
      }
    }
    
    jsonBody match {
      case Some(json) =>
        logger.debug(s"Successfully parsed JSON: $json")
        Ok(json)
      case None =>
        logger.error("Failed to parse request as JSON")
        BadRequest(Json.obj("error" -> "Could not parse request body as JSON"))
    }
  }
  
  // Debug with any content
  def debugText = Action(parse.anyContent) { request =>
    val textBody = request.body.asText.getOrElse {
      request.body.asRaw.map { raw => 
        raw.asBytes().getOrElse(ByteString.empty).utf8String 
      }.getOrElse("")
    }
    
    logger.debug(s"Text request received: '$textBody'")
    
    try {
      val json = Json.parse(textBody)
      Ok(json)
    } catch {
      case e: Exception => 
        logger.error(s"Failed to parse JSON: ${e.getMessage}")
        BadRequest(Json.obj("error" -> s"Invalid JSON: ${e.getMessage}"))
    }
  }
}

