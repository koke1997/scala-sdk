package services.data

import javax.inject._
import play.api.Logger
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scala.io.Source
import scala.jdk.CollectionConverters._  // Add this import for Java <-> Scala conversions
import java.io.File
import org.apache.commons.csv.{CSVFormat, CSVParser}
import java.nio.charset.StandardCharsets

/**
 * Lightweight data processing service
 */
@Singleton
class DataService @Inject()(
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  
  /**
   * Read CSV file and return as JSON
   */
  def readCsv(path: String, hasHeader: Boolean = true): Future[JsValue] = Future {
    logger.info(s"Reading CSV file: $path")
    
    val file = new File(path)
    if (!file.exists() || !file.isFile) {
      throw new IllegalArgumentException(s"File not found: $path")
    }
    
    // Using the non-deprecated approach for headers
    val format = if (hasHeader) {
      CSVFormat.DEFAULT.withHeader()
    } else {
      CSVFormat.DEFAULT
    }
    
    val parser = CSVParser.parse(file, StandardCharsets.UTF_8, format)
    
    try {
      if (hasHeader) {
        // Get the headers
        val headers = parser.getHeaderNames.asScala.toArray
        
        // Process records - convert Java List to Scala Seq
        val records = parser.getRecords.asScala
        val rows = records.map { record => 
          val obj = headers.zipWithIndex.map { case (header, i) =>
            header -> JsString(record.get(i))
          }.toMap
          JsObject(obj)
        }
        
        Json.obj(
          "columns" -> headers,
          "rows" -> rows,
          "count" -> rows.size
        )
      } else {
        // No headers, just return as arrays - convert Java List to Scala Seq
        val records = parser.getRecords.asScala
        val rows = records.map { record => 
          val values = (0 until record.size()).map(i => JsString(record.get(i)))
          JsArray(values)
        }
        
        Json.obj(
          "rows" -> rows,
          "count" -> rows.size
        )
      }
    } finally {
      parser.close()
    }
  }
  
  /**
   * Extract keywords from text
   */
  def extractKeywords(text: String, topN: Int = 5): Future[Seq[String]] = Future {
    logger.info(s"Extracting keywords from text, top $topN")
    
    // Simple keyword extraction (stopword filtering + frequency)
    val stopwords = Set("a", "an", "the", "and", "or", "but", "is", "are", "was", 
                      "were", "be", "been", "being", "have", "has", "had", "do", 
                      "does", "did", "to", "at", "by", "for", "with", "about")
    
    // Tokenize
    val tokens = text.toLowerCase
                  .replaceAll("[^a-zA-Z0-9\\s]", " ")
                  .split("\\s+")
                  .filter(_.length > 2)
                  .filterNot(stopwords.contains)
    
    // Count frequency
    val wordCounts = tokens.groupBy(identity).view.mapValues(_.length).toSeq
    
    // Get top N
    wordCounts.sortBy(-_._2).take(topN).map(_._1)
  }
}