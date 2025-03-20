package models

import play.api.libs.json._

/**
 * Response models for the Model Context Protocol (MCP).
 * These models represent the various API responses used by the application.
 */

/**
 * Response model for context operations
 *
 * @param id      Unique identifier for the context
 * @param status  Status of the operation (success, error, etc.)
 * @param data    Optional data payload
 * @param message Optional message providing additional information
 */
case class ContextOperationResponse(
  id: String,
  status: String,
  data: Option[ContextData] = None,
  message: Option[String] = None
)

object ContextOperationResponse {
  implicit val format: Format[ContextOperationResponse] = Json.format[ContextOperationResponse]
}

/**
 * Models for search results.
 *
 * @param results    Collection of context data results
 * @param totalCount Total number of results available
 * @param limit      Maximum number of results per page
 * @param offset     Starting offset of the results
 */
case class SearchResult(
  results: Seq[ContextData],
  totalCount: Int,
  limit: Int,
  offset: Int
)

object SearchResult {
  implicit val format: Format[SearchResult] = Json.format[SearchResult]
}

/**
 * Error response model
 *
 * @param error   Error message
 * @param code    Error code (optional)
 * @param details Additional error details (optional)
 */
case class ErrorResponse(
  error: String,
  code: Option[String] = None,
  details: Option[JsValue] = None
)

object ErrorResponse {
  implicit val format: Format[ErrorResponse] = Json.format[ErrorResponse]
}
