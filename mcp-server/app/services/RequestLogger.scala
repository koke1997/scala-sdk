package services

import play.api.libs.json._
import scala.collection.mutable
import java.util.concurrent.ConcurrentLinkedDeque
import scala.jdk.CollectionConverters._

/**
 * Simple in-memory request logger to track API activity.
 */
object RequestLogger {
  
  // Maximum number of requests to keep in memory
  private val MaxRequests = 100
  
  // Thread-safe collection for requests
  private val requests = new ConcurrentLinkedDeque[JsObject]()
  
  /**
   * Log a new request/response pair
   */
  def logRequest(method: String, path: String, requestBody: Option[JsValue], 
                 status: Int, responseBody: JsValue): Unit = {
    
    // Convert all values to proper JsValue types
    val entry = Json.obj(
      "timestamp" -> JsNumber(System.currentTimeMillis()),
      "method" -> JsString(method),
      "path" -> JsString(path),
      "statusCode" -> JsNumber(status),
      "requestBody" -> (requestBody.getOrElse(JsNull): JsValue),
      "responseBody" -> responseBody
    )
    
    // Add to the front of the list
    requests.addFirst(entry)
    
    // Trim if needed
    while (requests.size() > MaxRequests) {
      requests.removeLast()
    }
  }
  
  /**
   * Get recent requests as JSON array
   */
  def getRecentRequests(): JsArray = {
    JsArray(requests.iterator().asScala.toSeq)
  }
  
  /**
   * Clear all logged requests
   */
  def clearRequests(): Unit = {
    requests.clear()
  }
}

