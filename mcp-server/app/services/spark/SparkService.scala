package services.spark

import javax.inject._
import play.api.Logger
import play.api.inject.ApplicationLifecycle
import org.apache.spark.sql.{SparkSession, DataFrame}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for Apache Spark operations
 */
@Singleton
class SparkService @Inject()(lifecycle: ApplicationLifecycle)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  
  // Initialize Spark session
  private val spark: SparkSession = SparkSession.builder()
    .appName("MCP Server")
    .master("local[*]") // Use local mode for development
    .config("spark.driver.memory", "4g")
    .config("spark.ui.enabled", "false") // Disable Spark UI for simplicity
    .getOrCreate()
  
  logger.info(s"Created Spark session: ${spark.sparkContext.applicationId}")
  
  // Register shutdown hook to stop Spark gracefully
  lifecycle.addStopHook { () =>
    logger.info("Stopping Spark session")
    Future.successful(spark.stop())
  }
  
  /**
   * Read data from CSV file
   */
  def readCsv(path: String, header: Boolean = true): DataFrame = {
    logger.info(s"Reading CSV data from: $path")
    spark.read
      .option("header", header)
      .option("inferSchema", "true")
      .csv(path)
  }
  
  /**
   * Read data from JSON file
   */
  def readJson(path: String): DataFrame = {
    logger.info(s"Reading JSON data from: $path")
    spark.read.json(path)
  }
  
  /**
   * Execute SQL query on DataFrame
   */
  def sql(query: String): DataFrame = {
    logger.info(s"Executing SQL query: $query")
    spark.sql(query)
  }
  
  /**
   * Get Spark session
   */
  def getSparkSession: SparkSession = spark
}

