package services

import javax.inject._
import play.api.Logger
import play.api.libs.json._
import play.api.libs.ws.WSClient
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Service for handling streaming responses from API
 */
@Singleton
class StreamingService @Inject()(
  ws: WSClient,
  config: play.api.Configuration
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  private val ollamaBaseUrl = config.getOptional[String]("ollama.baseUrl").getOrElse("http://localhost:11434/api")
  private val requestTimeout = config.getOptional[Int]("http.timeout.request").getOrElse(300)
  
  /**
   * Stream generation results from Ollama
   */
  def streamGeneration(request: JsValue): Future[JsValue] = {
    logger.info("Stream generation request")
    logger.debug(s"Request body: ${Json.stringify(request)}")
    
    // Ensure the request has stream: true
    val streamRequest = request.as[JsObject] + ("stream" -> JsBoolean(true))
    
    ws.url(s"$ollamaBaseUrl/generate")
      .withRequestTimeout(requestTimeout.seconds)  // Use Scala Duration
      .post(streamRequest)
      .map(response => response.json)
      .recover {
        case e: Exception =>
          logger.error(s"Stream error: ${e.getMessage}", e)
          Json.obj("error" -> s"Streaming failed: ${e.getMessage}")
      }
  }
  
  /**
   * Stream generation results from text input
   */
  def streamGenerationFromText(text: String): Future[JsValue] = {
    logger.info(s"Stream generation from text, length: ${text.length}")
    
    val jsonBody = Json.obj(
      "model" -> "phi4-mini",
      "prompt" -> text,
      "stream" -> true
    )
    
    streamGeneration(jsonBody)
  }
}