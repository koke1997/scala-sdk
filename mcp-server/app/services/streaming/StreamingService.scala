package services.streaming

import core.config.OllamaConfig
import javax.inject._
import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.ExecutionContext

/**
 * Service for handling streaming responses
 */
@Singleton
class StreamingService @Inject()(
  streamingClient: OllamaStreamingClient,
  config: OllamaConfig
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  
  /**
   * Stream text generation from JSON request
   */
  def streamGeneration(request: JsValue): Source[ByteString, _] = {
    val endpoint = s"${config.baseUrl}/generate"
    streamingClient.createJsonStream(request, endpoint)
  }
  
  /**
   * Stream text generation from plain text
   */
  def streamGenerationFromText(text: String): Source[ByteString, _] = {
    val endpoint = s"${config.baseUrl}/generate"
    streamingClient.createTextStream(text, endpoint)
  }
}

