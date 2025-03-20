package models

import play.api.libs.json._

/**
 * Core domain models for the Model Context Protocol (MCP).
 * These models represent the core data structures used throughout the application.
 */

/**
 * Context data model that contains the actual content and metadata.
 *
 * @param id       Unique identifier for this context data
 * @param content  The actual content/text of the context
 * @param metadata Additional metadata as key-value pairs
 */
case class ContextData(
  id: String,
  content: String,
  metadata: Map[String, String] = Map.empty
)

object ContextData {
  implicit val format: Format[ContextData] = Json.format[ContextData]
}

// Base context request model
case class ContextRequest(
  text: String,
  metadata: Option[JsObject] = None,
  settings: Option[JsObject] = None
)

object ContextRequest {
  implicit val format: Format[ContextRequest] = Json.format[ContextRequest]
}

// Response model
case class ContextResponse(
  id: String,
  status: String,
  summary: Option[String] = None,
  metadata: Option[JsObject] = None
)

object ContextResponse {
  implicit val format: Format[ContextResponse] = Json.format[ContextResponse]
}

// Ollama models
case class OllamaRequest(
  prompt: String,
  model: String,
  stream: Option[Boolean] = Some(false),
  options: Option[OllamaOptions] = None,
  context: Option[Seq[Int]] = None
)

case class OllamaOptions(
  temperature: Option[Double] = None,
  top_p: Option[Double] = None,
  top_k: Option[Int] = None
)

object OllamaOptions {
  implicit val format: Format[OllamaOptions] = Json.format[OllamaOptions]
}

object OllamaRequest {
  implicit val format: Format[OllamaRequest] = Json.format[OllamaRequest]
}

case class OllamaResponse(
  model: String,
  response: String,
  context: Option[Seq[Int]] = None,
  done: Boolean
)

object OllamaResponse {
  implicit val format: Format[OllamaResponse] = Json.format[OllamaResponse]
}

// Claude Desktop specific models
case class ClaudeContextRequest(
  text: String,
  user: Option[String] = None,
  metadata: Option[JsObject] = None
)

object ClaudeContextRequest {
  implicit val format: Format[ClaudeContextRequest] = Json.format[ClaudeContextRequest]
}

case class ClaudeContextResponse(
  id: String,
  status: String = "processed",
  summary: Option[String] = None
)

object ClaudeContextResponse {
  implicit val format: Format[ClaudeContextResponse] = Json.format[ClaudeContextResponse]
}

/**
 * Session details model for tracking user sessions.
 *
 * @param sessionId Unique identifier for the session
 * @param userId    Optional user identifier
 * @param timestamp Session timestamp (epoch milliseconds)
 */
case class SessionDetails(
  sessionId: String,
  userId: Option[String],
  timestamp: Long
)

object SessionDetails {
  implicit val format: Format[SessionDetails] = Json.format[SessionDetails]
}
