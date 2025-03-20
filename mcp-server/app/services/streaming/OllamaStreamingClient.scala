package services.streaming

import javax.inject._
import play.api.Logger
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.json._
import play.api.libs.ws.WSClient
import core.config.{OllamaConfig, HttpConfig}
import core.streaming.StreamHandler
import org.asynchttpclient.{AsyncHttpClient, DefaultAsyncHttpClient, Request, Response}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Try, Success, Failure}

/**
 * Client for handling streaming responses from Ollama
 */
@Singleton
class OllamaStreamingClient @Inject()(
  ws: WSClient,
  ollamaConfig: OllamaConfig,
  httpConfig: HttpConfig
)(implicit ec: ExecutionContext) extends StreamHandler {
  
  private val logger = Logger(getClass)
  private lazy val asyncHttpClient = new DefaultAsyncHttpClient()
  
  // Convert Scala Duration to Java Duration when needed
  private def toJavaDuration(duration: FiniteDuration): java.time.Duration = {
    java.time.Duration.ofMillis(duration.toMillis)
  }
  
  /**
   * Create a streaming source for JSON requests
   */
  override def createJsonStream(request: JsValue, endpoint: String): Source[ByteString, _] = {
    logger.info(s"Setting up streaming request to $endpoint")
    
    // Ensure streaming is enabled
    val streamRequest = request.as[JsObject] + ("stream" -> JsBoolean(true))
    logger.debug(s"Request body: ${Json.stringify(streamRequest)}")
    
    // Create request body
    val requestBytes = ByteString(Json.stringify(streamRequest))
    
    // Create the actual stream
    createStreamFromRequest(endpoint, requestBytes)
  }
  
  /**
   * Create a streaming source for text requests
   */
  override def createTextStream(text: String, endpoint: String): Source[ByteString, _] = {
    logger.info(s"Setting up streaming text request to $endpoint")
    
    val jsonBody = Json.obj(
      "model" -> ollamaConfig.defaultModel,
      "prompt" -> text,
      "stream" -> true
    )
    
    createJsonStream(jsonBody, endpoint)
  }
  
  /**
   * Helper to create a stream from a request
   */
  private def createStreamFromRequest(endpoint: String, requestBytes: ByteString): Source[ByteString, _] = {
    Source.single(requestBytes)
      .map { body =>
        Try {
          val request = asyncHttpClient.preparePost(endpoint)
            .addHeader("Content-Type", "application/json")
            .setRequestTimeout(toJavaDuration(httpConfig.requestTimeout))  // Convert to Java Duration
            .setBody(body.utf8String)
            .build()
          
          asyncHttpClient.executeRequest(request)
            .toCompletableFuture
            .get()  // This blocks - not ideal
        } match {
          case Success(response) => response
          case Failure(e) =>
            logger.error(s"Error setting up stream: ${e.getMessage}", e)
            throw e
        }
      }
      .flatMapConcat { response =>
        if (response.getStatusCode >= 400) {
          Source.single(ByteString(s"""{"error":"HTTP ${response.getStatusCode} ${response.getStatusText}"}"""))
        } else {
          // Use the correct method for getting response body
          Source.single(ByteString(response.getResponseBody))
        }
      }
      .recover {
        case e: Exception =>
          logger.error(s"Stream error: ${e.getMessage}", e)
          ByteString(s"""{"error":"${e.getMessage.replace("\"", "\\\"")}"}""")
      }
  }
}