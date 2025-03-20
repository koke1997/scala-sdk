package controllers

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient
import org.apache.pekko.util.ByteString
import org.apache.pekko.stream.scaladsl._
import play.api.http.HttpEntity
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClient, Request}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

@Singleton
class OllamaController @Inject()(
  cc: ControllerComponents,
  ws: WSClient,
  config: play.api.Configuration
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(this.getClass)
  private val ollamaBaseUrl = config.getOptional[String]("ollama.baseUrl").getOrElse("http://localhost:11434/api")
  private lazy val asyncHttpClient = new DefaultAsyncHttpClient()
  
  // List available models
  def listModels() = Action.async {
    logger.info("Requesting list of Ollama models")
    
    ws.url(s"$ollamaBaseUrl/tags")
      .withRequestTimeout(2.minutes)  // Use Scala Duration
      .get()
      .map { response => 
        logger.info(s"Ollama models response: ${response.body.take(100)}...")
        Ok(response.json)
      }
      .recover {
        case e: Exception => 
          logger.error(s"Error listing models: ${e.getMessage}", e)
          InternalServerError(Json.obj("error" -> s"Failed to fetch models: ${e.getMessage}"))
      }
  }
  
  // Generate text with streaming support
  def generateStream() = Action(parse.raw) { request =>
    logger.info("Generate stream request received")
    
    // Get content as bytes and convert to string
    val bytes = request.body.asBytes().getOrElse(ByteString.empty)
    
    if (bytes.isEmpty) {
      logger.warn("Empty request body")
      BadRequest(Json.obj("error" -> "Empty request body"))
    } else {
      val bodyString = bytes.utf8String
      
      val requestBodyJson = Try(Json.parse(bodyString)).getOrElse {
        Json.obj(
          "model" -> "phi4-mini", 
          "prompt" -> bodyString.take(500),
          "stream" -> true
        )
      }
      
      // Add streaming if not explicitly set
      val finalRequestBody = if (!(requestBodyJson \ "stream").toOption.contains(JsBoolean(false))) {
        requestBodyJson.as[JsObject] + ("stream" -> JsBoolean(true))
      } else {
        requestBodyJson
      }
      
      logger.info(s"Setting up streaming request to Ollama API: $ollamaBaseUrl/generate")
      
      // Set up streaming source using AsyncHttpClient directly
      val responseSource = Source.single(ByteString(Json.stringify(finalRequestBody)))
        .map { requestBody =>
          val request = asyncHttpClient.preparePost(s"$ollamaBaseUrl/generate")
            .setHeader("Content-Type", "application/json")
            .setBody(requestBody.utf8String)
            .build()
          
          Try {
            // Execute the request
            asyncHttpClient.executeRequest(request).get()
          } match {
            case Success(response) => response
            case Failure(e) =>
              logger.error(s"Failed to execute request: ${e.getMessage}", e)
              throw e
          }
        }
        .flatMapConcat { response =>
          if (response.getStatusCode >= 400) {
            logger.error(s"HTTP error: ${response.getStatusCode} ${response.getStatusText}")
            Source.single(ByteString(s"""{"error":"HTTP ${response.getStatusCode} ${response.getStatusText}"}"""))
          } else {
            // Convert AsyncHttpClient response to a Source
            Source.unfoldAsync(response) { resp =>
              // Read from the response body in chunks
              if (resp.hasResponseBody()) {
                Future.successful(Some((resp, ByteString(resp.getResponseBody))))
              } else {
                Future.successful(None)
              }
            }
          }
        }
        .recover {
          case e: Exception =>
            logger.error(s"Stream error: ${e.getMessage}", e)
            ByteString(s"""{"error":"${e.getMessage.replace("\"", "\\\"")}"}""")
        }
      
      Ok.chunked(responseSource).as("application/json")
    }
  }
  
  // Simple text endpoint - with longer timeouts
  def generateText() = Action(parse.text).async { request =>
    logger.info(s"Generate text request received, length: ${request.body.length}")
    
    val jsonBody = Json.obj(
      "model" -> "phi4-mini", 
      "prompt" -> request.body
    )
    
    logger.info(s"Sending Ollama generate-text request to: $ollamaBaseUrl/generate")
    
    ws.url(s"$ollamaBaseUrl/generate")
      .withRequestTimeout(5.minutes)  // Use Scala Duration
      .post(jsonBody)
      .map { response => 
        logger.info(s"Ollama response received, status: ${response.status}")
        logger.debug(s"Response body: ${response.body.take(200)}...")
        Ok(response.json)
      }
      .recover {
        case e: Exception =>
          logger.error(s"Error calling Ollama API: ${e.getMessage}", e)
          InternalServerError(Json.obj(
            "error" -> s"API error: ${e.getMessage}",
            "type" -> "timeout",
            "suggestion" -> "This might be due to the LLM taking too long to respond. Consider using streaming mode for better user experience."
          ))
      }
  }
  
  // Debug controller for inspecting requests
  def debug() = Action(parse.raw) { request =>
    val bytes = request.body.asBytes().getOrElse(ByteString.empty)
    val bodyText = bytes.utf8String
    
    Ok(Json.obj(
      "contentType" -> Json.toJson(request.contentType.getOrElse("none")),
      "bodyLength" -> Json.toJson(bytes.size),
      "bodyText" -> Json.toJson(bodyText),
      "headers" -> Json.toJson(request.headers.headers.map(h => h._1 + ": " + h._2).mkString("\n")),
      "timestamp" -> Json.toJson(System.currentTimeMillis())
    ))
  }
}