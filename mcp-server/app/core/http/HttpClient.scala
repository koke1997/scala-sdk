package core.http

import play.api.libs.json._
import scala.concurrent.{ExecutionContext, Future}

/**
 * Simple HTTP client interface for basic operations
 */
trait HttpClient {
  implicit def executionContext: ExecutionContext
  
  /**
   * Simple GET request
   */
  def get(url: String): Future[JsValue]
  
  /**
   * Simple POST request with JSON body
   */
  def post(url: String, body: JsValue): Future[JsValue]
  
  /**
   * POST request with text body
   */
  def postText(url: String, body: String): Future[JsValue]
  
  /**
   * POST request for streaming
   */
  def postStream(url: String, body: JsValue): Future[JsValue]
}