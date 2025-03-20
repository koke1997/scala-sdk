package controllers.api

import javax.inject._
import play.api.mvc._
import play.api.Logger
import play.api.libs.json._
import services.spark.SparkService

import scala.concurrent.ExecutionContext
import scala.util.{Try, Success, Failure}

/**
 * Controller for Apache Spark operations
 */
@Singleton
class SparkController @Inject()(
  cc: ControllerComponents,
  sparkService: SparkService
)(implicit ec: ExecutionContext) extends AbstractController(cc) {
  
  private val logger = Logger(getClass)
  
  /**
   * Query data using Spark SQL
   */
  def query() = Action(parse.json) { request =>
    val query = (request.body \ "query").asOpt[String]
    val path = (request.body \ "path").asOpt[String]
    val format = (request.body \ "format").asOpt[String].getOrElse("csv")
    
    if (query.isEmpty || path.isEmpty) {
      BadRequest(Json.obj("error" -> "Query and path are required"))
    } else {
      Try {
        // Read data
        val df = format.toLowerCase match {
          case "csv" => sparkService.readCsv(path.get)
          case "json" => sparkService.readJson(path.get)
          case _ => throw new IllegalArgumentException(s"Unsupported format: $format")
        }
        
        // Register as temp view
        df.createOrReplaceTempView("data")
        
        // Execute query
        val result = sparkService.sql(query.get)
        
        // Convert result to JSON
        val rows = result.toJSON.collectAsList()
        val jsonRows = (0 until rows.size()).map(i => Json.parse(rows.get(i).toString))
        
        Ok(Json.obj(
          "columns" -> result.columns.toSeq,
          "rows" -> jsonRows,
          "count" -> result.count()
        ))
      } match {
        case Success(result) => result
        case Failure(e) =>
          logger.error(s"Error executing Spark query: ${e.getMessage}", e)
          InternalServerError(Json.obj("error" -> s"Query error: ${e.getMessage}"))
      }
    }
  }
  
  /**
   * Get basic information about the Spark environment
   */
  def info() = Action {
    val spark = sparkService.getSparkSession
    val sc = spark.sparkContext
    
    Ok(Json.obj(
      "applicationId" -> sc.applicationId,
      "master" -> sc.master,
      "sparkVersion" -> sc.version,
      "defaultParallelism" -> sc.defaultParallelism,
      "executorMemory" -> sc.getConf.get("spark.executor.memory", "N/A"),
      "driverMemory" -> sc.getConf.get("spark.driver.memory", "N/A")
    ))
  }
}

