# Initialize-MCPProject.ps1
# This script initializes the MCP server project structure and base files

# Set error action preference to stop on errors
$ErrorActionPreference = "Stop"

# Define project name and base directory
$projectName = "mcp-server"
$baseDir = Join-Path $PWD $projectName

# Create base directory if it doesn't exist
Write-Host "Creating project directory structure..." -ForegroundColor Green
if (Test-Path $baseDir) {
    Write-Host "Directory $baseDir already exists. Do you want to continue and potentially overwrite files? (y/n)" -ForegroundColor Yellow
    $confirm = Read-Host
    if ($confirm -ne "y") {
        Write-Host "Operation cancelled." -ForegroundColor Red
        exit
    }
} else {
    New-Item -Path $baseDir -ItemType Directory | Out-Null
}

# Create directory structure
$directories = @(
    "app/async",
    "app/controllers",
    "app/models",
    "app/services",
    "app/utils",
    "conf",
    "public/assets/css",
    "public/assets/js",
    "public/assets/images",
    "project",
    "test/controllers",
    "test/models",
    "test/services",
    "test/utils"
)

foreach ($dir in $directories) {
    $path = Join-Path $baseDir $dir
    if (-not (Test-Path $path)) {
        New-Item -Path $path -ItemType Directory | Out-Null
    }
}

# Create base files
$files = @{
    # App files
    "app/async/ZioTaskProcessor.scala" = @"
package async

import zio._

/**
 * Handles asynchronous processing of tasks using ZIO.
 */
object ZioTaskProcessor {
  /**
   * Processes a task asynchronously.
   *
   * @param task The task to be processed.
   * @return A ZIO effect that represents the asynchronous processing.
   */
  def processTask[A, B](task: A)(processFunction: A => B): Task[B] = {
    ZIO.attempt(processFunction(task))
  }
}
"@

    "app/controllers/BaseController.scala" = @"
package controllers

import play.api.mvc._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Base controller that provides common functionality for all controllers.
 */
abstract class BaseController(cc: ControllerComponents)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  /**
   * Helper method to handle JSON requests.
   */
  def withJsonBody[A](f: A => Future[Result])(implicit request: Request[JsValue], reads: Reads[A]): Future[Result] = {
    request.body.validate[A].fold(
      errors => {
        Future.successful(BadRequest(Json.obj("error" -> JsError.toJson(errors))))
      },
      model => f(model)
    )
  }
  
  /**
   * Helper method for returning standardized error responses.
   */
  def errorResponse(message: String, status: Int = 400): Result = {
    Status(status)(Json.obj("error" -> message))
  }
}
"@

    "app/controllers/MCPController.scala" = @"
package controllers

import javax.inject._
import play.api.mvc._
import play.api.libs.json._
import services.MCPService
import models.{ContextRequest, ContextResponse}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Controller that handles MCP API endpoints.
 */
@Singleton
class MCPController @Inject()(
  cc: ControllerComponents,
  mcpService: MCPService
)(implicit ec: ExecutionContext) extends BaseController(cc) {

  /**
   * Endpoint to update context data.
   */
  def updateContext: Action[JsValue] = Action.async(parse.json) { implicit request =>
    withJsonBody[ContextRequest] { contextRequest =>
      mcpService.processContext(contextRequest).map { result =>
        Ok(Json.toJson(result))
      }.recover {
        case e: Exception => errorResponse(e.getMessage, 500)
      }
    }
  }

  /**
   * Endpoint to fetch context data.
   */
  def fetchContext(id: String): Action[AnyContent] = Action.async { implicit request =>
    mcpService.getContext(id).map {
      case Some(result) => Ok(Json.toJson(result))
      case None => NotFound(Json.obj("error" -> s"Context with ID $id not found"))
    }.recover {
      case e: Exception => errorResponse(e.getMessage, 500)
    }
  }
}
"@

    "app/models/ContextModels.scala" = @"
package models

import play.api.libs.json._

/**
 * Domain models for context data.
 */
case class ContextData(
  id: String,
  content: String,
  metadata: Map[String, String] = Map.empty
)

object ContextData {
  implicit val format: Format[ContextData] = Json.format[ContextData]
}

/**
 * Session details model.
 */
case class SessionDetails(
  sessionId: String,
  userId: Option[String],
  timestamp: Long
)

object SessionDetails {
  implicit val format: Format[SessionDetails] = Json.format[SessionDetails]
}
"@

    "app/models/RequestModels.scala" = @"
package models

import play.api.libs.json._

/**
 * Models for incoming requests.
 */
case class ContextRequest(
  contextData: ContextData,
  sessionDetails: SessionDetails
)

object ContextRequest {
  implicit val format: Format[ContextRequest] = Json.format[ContextRequest]
}

/**
 * Models for search or filter requests.
 */
case class SearchRequest(
  query: String,
  filters: Map[String, String] = Map.empty,
  limit: Int = 10,
  offset: Int = 0
)

object SearchRequest {
  implicit val format: Format[SearchRequest] = Json.format[SearchRequest]
}
"@

    "app/models/ResponseModels.scala" = @"
package models

import play.api.libs.json._

/**
 * Models for outgoing responses.
 */
case class ContextResponse(
  id: String,
  status: String,
  data: Option[ContextData] = None,
  message: Option[String] = None
)

object ContextResponse {
  implicit val format: Format[ContextResponse] = Json.format[ContextResponse]
}

/**
 * Models for search results.
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
"@

    "app/services/MCPService.scala" = @"
package services

import javax.inject._
import models.{ContextData, ContextRequest, ContextResponse, SessionDetails}
import async.ZioTaskProcessor
import zio._
import zio.interop.catz._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service that handles MCP business logic.
 */
@Singleton
class MCPService @Inject()(
  implicit ec: ExecutionContext
) {
  // In-memory storage for demo purposes
  // In production, you would use a proper database
  private val contextStore = scala.collection.mutable.Map[String, ContextData]()

  /**
   * Process incoming context data.
   */
  def processContext(request: ContextRequest): Future[ContextResponse] = {
    val contextData = request.contextData
    
    // Example of using ZIO for async processing
    val task = ZioTaskProcessor.processTask(contextData) { data =>
      // Simulate processing
      Thread.sleep(100)
      contextStore.put(data.id, data)
      data
    }
    
    // Convert ZIO Task to Future
    val runtime = Runtime.default
    runtime.unsafeRunToFuture(task).map { processedData =>
      ContextResponse(
        id = processedData.id,
        status = "success",
        data = Some(processedData),
        message = Some("Context processed successfully")
      )
    }.recover {
      case e: Exception =>
        ContextResponse(
          id = contextData.id,
          status = "error",
          message = Some(e.getMessage)
        )
    }
  }

  /**
   * Retrieve context data by ID.
   */
  def getContext(id: String): Future[Option[ContextData]] = {
    Future.successful(contextStore.get(id))
  }
}
"@

    "app/services/AuthService.scala" = @"
package services

import javax.inject._
import play.api.mvc.{Request, Result, Results}
import play.api.libs.json.Json

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for handling authentication and authorization.
 */
@Singleton
class AuthService @Inject()(
  implicit ec: ExecutionContext
) {
  // In a real application, you would implement proper token validation
  private val validTokens = Set("test-token-1", "test-token-2")

  /**
   * Validates the API token in the request header.
   */
  def validateToken(request: Request[_]): Boolean = {
    request.headers.get("X-API-Token").exists(validTokens.contains)
  }

  /**
   * Authentication action filter.
   */
  def withAuth(block: => Future[Result])(implicit request: Request[_]): Future[Result] = {
    if (validateToken(request)) {
      block
    } else {
      Future.successful(Results.Unauthorized(Json.obj("error" -> "Invalid or missing API token")))
    }
  }
}
"@

    "app/utils/ConfigUtils.scala" = @"
package utils

import com.typesafe.config.{Config, ConfigFactory}
import javax.inject._

/**
 * Utility for working with configuration.
 */
@Singleton
class ConfigUtils @Inject()() {
  private val config: Config = ConfigFactory.load()

  /**
   * Gets a string configuration value.
   */
  def getString(path: String, default: String = ""): String = {
    if (config.hasPath(path)) config.getString(path) else default
  }

  /**
   * Gets an integer configuration value.
   */
  def getInt(path: String, default: Int = 0): Int = {
    if (config.hasPath(path)) config.getInt(path) else default
  }

  /**
   * Gets a boolean configuration value.
   */
  def getBoolean(path: String, default: Boolean = false): Boolean = {
    if (config.hasPath(path)) config.getBoolean(path) else default
  }
}
"@

    "app/utils/JsonUtils.scala" = @"
package utils

import play.api.libs.json._

/**
 * Utility functions for working with JSON.
 */
object JsonUtils {
  /**
   * Transforms JSON by applying a function to specified fields.
   */
  def transformJson[T](json: JsValue, fieldNames: Seq[String], f: JsValue => JsValue)
    (implicit reads: Reads[T], writes: Writes[T]): JsValue = {
    
    json.validate[T].fold(
      _ => json,
      valid => {
        val obj = json.as[JsObject]
        val transformed = fieldNames.foldLeft(obj) { (res, field) =>
          (res \ field).toOption match {
            case Some(value) => res + (field -> f(value))
            case None => res
          }
        }
        transformed
      }
    )
  }
}
"@

    # Configuration files
    "conf/application.conf" = @"
# https://www.playframework.com/documentation/latest/Configuration

# Application configuration
play.http.secret.key="changeme"
play.http.errorHandler = "play.api.http.JsonHttpErrorHandler"

# Database configuration - uncomment and configure if needed
# slick.dbs.default.profile="slick.jdbc.H2Profile$"
# slick.dbs.default.db.driver="org.h2.Driver"
# slick.dbs.default.db.url="jdbc:h2:mem:play"

# Filters
play.filters {
  enabled += "play.filters.cors.CORSFilter"
  cors {
    allowedOrigins = ["localhost:9000", "localhost:3000"]
    allowedHttpMethods = ["GET", "POST", "PUT", "DELETE", "OPTIONS"]
    allowedHttpHeaders = ["Accept", "Content-Type", "Origin", "X-API-Token"]
  }
}

# Additional application-specific configuration
mcp {
  # Example configuration for application-specific settings
  timeouts {
    default = 30 seconds
    longRunning = 5 minutes
  }
}
"@

    "conf/routes" = @"
# Routes
# This file defines all application routes (Higher priority routes first)
# https://www.playframework.com/documentation/latest/ScalaRouting

# API endpoints
POST    /api/context/update           controllers.MCPController.updateContext
GET     /api/context/:id              controllers.MCPController.fetchContext(id: String)

# Map static resources from the /public folder to the /assets URL path
GET     /assets/*file                 controllers.Assets.versioned(path="/public", file: Asset)
"@

    "conf/logback.xml" = @"
<!-- https://www.playframework.com/documentation/latest/SettingsLogger -->
<configuration>

  <appender name="FILE" class="ch.qos.logback.core.FileAppender">
    <file>logs/application.log</file>
    <encoder>
      <pattern>%date [%level] from %logger in %thread - %message%n%xException</pattern>
    </encoder>
  </appender>

  <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
    <encoder>
      <pattern>%highlight(%date [%level] %logger{15}) - %message%n%xException{10}</pattern>
    </encoder>
  </appender>

  <appender name="ASYNCFILE" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="FILE" />
  </appender>

  <appender name="ASYNCSTDOUT" class="ch.qos.logback.classic.AsyncAppender">
    <appender-ref ref="STDOUT" />
  </appender>

  <logger name="play" level="INFO" />
  <logger name="application" level="DEBUG" />
  <logger name="controllers" level="DEBUG" />
  <logger name="services" level="DEBUG" />

  <root level="INFO">
    <appender-ref ref="ASYNCFILE" />
    <appender-ref ref="ASYNCSTDOUT" />
  </root>

</configuration>
"@

    # Public files
    "public/index.html" = @"
<!DOCTYPE html>
<html>
<head>
    <title>MCP Server</title>
    <link rel="stylesheet" href="/assets/css/main.css">
</head>
<body>
    <div class="container">
        <h1>MCP Server</h1>
        <p>RESTful API backend for Claude Desktop</p>
    </div>
    <script src="/assets/js/main.js"></script>
</body>
</html>
"@

    "public/assets/css/main.css" = @"
body {
    font-family: 'Helvetica Neue', Arial, sans-serif;
    margin: 0;
    padding: 0;
    background: #f5f5f5;
    color: #333;
}

.container {
    max-width: 960px;
    margin: 0 auto;
    padding: 2rem;
}

h1 {
    color: #2c3e50;
}
"@

    "public/assets/js/main.js" = @"
// Main JavaScript file
console.log('MCP Server application loaded');
"@

    # Project files
    "project/build.properties" = @"
sbt.version=1.9.6
"@

    "project/plugins.sbt" = @"
// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.0")

// Formatting and style
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.8")
"@

    # Test files
    "test/controllers/MCPControllerSpec.scala" = @"
package controllers

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.libs.json._
import models.{ContextData, ContextRequest, SessionDetails}

/**
 * Test cases for the MCPController.
 */
class MCPControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "MCPController" should {

    "process an update context request" in {
      val controller = app.injector.instanceOf[MCPController]
      val contextData = ContextData("test-id", "Test content")
      val sessionDetails = SessionDetails("session-123", Some("user-456"), System.currentTimeMillis())
      val request = ContextRequest(contextData, sessionDetails)
      
      val requestJson = Json.toJson(request)
      val result = controller.updateContext.apply(FakeRequest(POST, "/api/context/update")
        .withHeaders(CONTENT_TYPE -> "application/json")
        .withJsonBody(requestJson))

      status(result) mustBe OK
      contentType(result) mustBe Some("application/json")
      (contentAsJson(result) \ "status").as[String] mustBe "success"
    }

    "return not found for a non-existent context id" in {
      val controller = app.injector.instanceOf[MCPController]
      val result = controller.fetchContext("non-existent-id").apply(FakeRequest())

      status(result) mustBe NOT_FOUND
      contentType(result) mustBe Some("application/json")
    }
  }
}
"@

    "test/models/MCPModelsSpec.scala" = @"
package models

import org.scalatestplus.play._
import play.api.libs.json._

/**
 * Test cases for model classes and JSON serialization.
 */
class MCPModelsSpec extends PlaySpec {

  "ContextData" should {
    "be correctly serialized to JSON" in {
      val contextData = ContextData("test-id", "Test content", Map("key1" -> "value1"))
      val json = Json.toJson(contextData)
      
      (json \ "id").as[String] mustBe "test-id"
      (json \ "content").as[String] mustBe "Test content"
      (json \ "metadata" \ "key1").as[String] mustBe "value1"
    }

    "be correctly deserialized from JSON" in {
      val json = Json.obj(
        "id" -> "test-id",
        "content" -> "Test content",
        "metadata" -> Json.obj("key1" -> "value1")
      )
      
      val contextData = json.as[ContextData]
      contextData.id mustBe "test-id"
      contextData.content mustBe "Test content"
      contextData.metadata mustBe Map("key1" -> "value1")
    }
  }

  "ContextResponse" should {
    "be correctly serialized to JSON" in {
      val contextData = ContextData("test-id", "Test content")
      val response = ContextResponse(
        id = "test-id",
        status = "success",
        data = Some(contextData),
        message = Some("Context processed successfully")
      )
      
      val json = Json.toJson(response)
      (json \ "id").as[String] mustBe "test-id"
      (json \ "status").as[String] mustBe "success"
      (json \ "data" \ "id").as[String] mustBe "test-id"
      (json \ "message").as[String] mustBe "Context processed successfully"
    }
  }
}
"@

    "test/services/MCPServiceSpec.scala" = @"
package services

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import models.{ContextData, ContextRequest, SessionDetails}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.concurrent.Await

/**
 * Test cases for the MCPService.
 */
class MCPServiceSpec extends PlaySpec with GuiceOneAppPerSuite {

  "MCPService" should {
    "process context data correctly" in {
      implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
      val service = new MCPService()
      
      val contextData = ContextData("test-id", "Test content")
      val sessionDetails = SessionDetails("session-123", Some("user-456"), System.currentTimeMillis())
      val request = ContextRequest(contextData, sessionDetails)
      
      val future = service.processContext(request)
      val result = Await.result(future, 5.seconds)
      
      result.id mustBe "test-id"
      result.status mustBe "success"
      result.data.isDefined mustBe true
      result.data.get.id mustBe "test-id"
      result.data.get.content mustBe "Test content"
    }

    "retrieve previously processed context data" in {
      implicit val ec: ExecutionContext = app.injector.instanceOf[ExecutionContext]
      val service = new MCPService()
      
      val contextData = ContextData("test-id-2", "Another test content")
      val sessionDetails = SessionDetails("session-456", Some("user-789"), System.currentTimeMillis())
      val request = ContextRequest(contextData, sessionDetails)
      
      // Process the context data first
      Await.result(service.processContext(request), 5.seconds)
      
      // Then try to retrieve it
      val future = service.getContext("test-id-2")
      val result = Await.result(future, 5.seconds)
      
      result.isDefined mustBe true
      result.get.id mustBe "test-id-2"
      result.get.content mustBe "Another test content"
    }
  }
}
"@

    "test/utils/JsonUtilsSpec.scala" = @"
package utils

import org.scalatestplus.play._
import play.api.libs.json._
import models.ContextData

/**
 * Test cases for JSON utility functions.
 */
class JsonUtilsSpec extends PlaySpec {

  "JsonUtils" should {
    "correctly transform specified fields" in {
      val contextData = ContextData("test-id", "Test content")
      val json = Json.toJson(contextData)
      
      // Uppercase the content field
      val transformed = JsonUtils.transformJson[ContextData](
        json,
        Seq("content"),
        value => JsString(value.as[String].toUpperCase())
      )
      
      (transformed \ "id").as[String] mustBe "test-id"
      (transformed \ "content").as[String] mustBe "TEST CONTENT"
    }
  }
}
"@

    # Root files
    "build.sbt" = @"
name := "mcp-server"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.12"

libraryDependencies ++= Seq(
  guice,
  "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.0" % Test,
  
  // ZIO for asynchronous processing
  "dev.zio" %% "zio" % "2.0.15",
  "dev.zio" %% "zio-interop-cats" % "23.0.0.8",
  
  // Optional: For database access (Slick)
  // "com.typesafe.play" %% "play-slick" % "5.1.0",
  // "com.typesafe.play" %% "play-slick-evolutions" % "5.1.0",
  // "com.h2database" % "h2" % "2.2.224",
  
  // For JSON handling
  "org.playframework" %% "play-json" % "3.0.1",
  
  // For HTTP client (optional)
  "org.playframework" %% "play-ahc-ws" % "3.0.0"
)

// Add scalac options
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked"
)

// Code coverage configuration
coverageEnabled := false
coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"
"@

    "README.md" = @"
# MCP Server

## Overview
MCP server is a RESTful API backend for Claude Desktop, handling context-based requests. The project is built with Scala, Play Framework, and ZIO for asynchronous processing.

## Requirements
- JDK 11+
- SBT 1.9.x
- Scala 2.13.x

## Getting Started

### Setup
1. Clone this repository
2. Navigate to the project directory
3. Run `sbt` to start the SBT shell
4. Run `compile` to compile the project
5. Run `run` to start the server locally

The server will be available at `http://localhost:9000`.

### API Endpoints
- POST `/api/context/update` - Updates context data
- GET `/api/context/:id` - Fetches context data by ID

### Testing
Run tests using:
```
sbt test
```

## Project Structure

### Key Components
- **Controller Layer** - Handles HTTP requests and responses
- **Service Layer** - Contains business logic
- **Model Layer** - Defines domain models
- **Async Layer** - Manages asynchronous processing with ZIO
- **Utils** - Provides utility functions

### Technologies
- Play Framework for RESTful APIs
- ZIO for asynchronous processing
- Play JSON for JSON handling
- Logback for logging

## Development

### Adding New Endpoints
1. Add new route in `conf/routes`
2. Create controller method in appropriate controller
3. Implement service method
4. Add tests

### Configuration
Application configuration is located in `conf/application.conf`.

## License
This project is proprietary and confidential.
"@
}

# Write files
Write-Host "Creating base files..." -ForegroundColor Green
foreach ($file in $files.Keys) {
    $filePath = Join-Path $baseDir $file
    $fileContent = $files[$file]
    
    # Create directory if doesn't exist
    $dir = Split-Path $filePath
    if (-not (Test-Path $dir)) {
        New-Item -Path $dir -ItemType Directory | Out-Null
    }
    
    # Write file
    Set-Content -Path $filePath -Value $fileContent
}

# Initialize git repository
Write-Host "Initializing git repository..." -ForegroundColor Green
Push-Location $baseDir
try {
    git init
    git add .
    git commit -m "Initial commit: Base project structure"
} catch {
    Write-Host "Git initialization failed. Make sure git is installed and available in your PATH." -ForegroundColor Yellow
} finally {
    Pop-Location
}

Write-Host "Project initialization complete!" -ForegroundColor Green
Write-Host "To get started:" -ForegroundColor Cyan
Write-Host "  1. cd $projectName" -ForegroundColor White
Write-Host "  2. sbt run" -ForegroundColor White
Write-Host "The server will be available at http://localhost:9000" -ForegroundColor White