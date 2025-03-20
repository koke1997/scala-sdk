package core.streaming

import org.apache.pekko.stream.scaladsl.Source
import org.apache.pekko.util.ByteString
import play.api.libs.json._

/**
 * Base interface for handling streaming responses
 */
trait StreamHandler {
  /**
   * Creates a source for streaming responses
   */
  def createJsonStream(request: JsValue, endpoint: String): Source[ByteString, _]
  
  /**
   * Creates a source for streaming text responses
   */
  def createTextStream(text: String, endpoint: String): Source[ByteString, _]
}

