package services.spark

import javax.inject._
import play.api.Logger
import org.apache.spark.sql.{DataFrame, Row}
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature.Tokenizer
import org.apache.spark.ml.feature.StopWordsRemover
import org.apache.spark.ml.feature.HashingTF
import org.apache.spark.ml.feature.IDF

import scala.concurrent.{ExecutionContext, Future}

/**
 * Service for data processing operations using Spark
 */
@Singleton
class DataProcessingService @Inject()(
  sparkService: SparkService
)(implicit ec: ExecutionContext) {
  
  private val logger = Logger(getClass)
  private val spark = sparkService.getSparkSession
  import spark.implicits._
  
  /**
   * Process text data for NLP tasks
   */
  def processTextData(texts: Seq[String], textColumn: String = "text"): DataFrame = {
    logger.info(s"Processing ${texts.size} text records")
    
    // Create DataFrame from texts
    val df = texts.toDF(textColumn)
    
    // Tokenize text
    val tokenizer = new Tokenizer()
      .setInputCol(textColumn)
      .setOutputCol("tokens")
    
    val tokenized = tokenizer.transform(df)
    
    // Remove stop words
    val remover = new StopWordsRemover()
      .setInputCol("tokens")
      .setOutputCol("filtered_tokens")
    
    val filtered = remover.transform(tokenized)
    
    // Calculate TF-IDF
    val hashingTF = new HashingTF()
      .setInputCol("filtered_tokens")
      .setOutputCol("raw_features")
      .setNumFeatures(1000)
    
    val featurized = hashingTF.transform(filtered)
    
    val idf = new IDF()
      .setInputCol("raw_features")
      .setOutputCol("features")
    
    val idfModel = idf.fit(featurized)
    val processed = idfModel.transform(featurized)
    
    processed
  }
  
  /**
   * Extract keywords from text
   */
  def extractKeywords(text: String, topN: Int = 5): Seq[String] = {
    logger.info(s"Extracting top $topN keywords from text")
    
    // Create DataFrame with single text
    val df = Seq(text).toDF("text")
    
    // Tokenize
    val tokenizer = new Tokenizer()
      .setInputCol("text")
      .setOutputCol("tokens")
    
    val tokenized = tokenizer.transform(df)
    
    // Remove stop words
    val remover = new StopWordsRemover()
      .setInputCol("tokens")
      .setOutputCol("filtered_tokens")
    
    val filtered = remover.transform(tokenized)
    
    // Count word frequencies
    val flattenedTokens = filtered
      .select(explode($"filtered_tokens").as("word"))
      .groupBy("word")
      .count()
      .orderBy(desc("count"))
      .limit(topN)
    
    // Extract top words
    flattenedTokens
      .select("word")
      .as[String]
      .collect()
      .toSeq
  }
  
  /**
   * Analyze sentiment of text (very basic implementation)
   */
  def analyzeSentiment(text: String): Double = {
    logger.info("Analyzing sentiment of text")
    
    // Create a list of positive and negative words (very simplified)
    val positiveWords = Set("good", "great", "excellent", "positive", "wonderful", "best", "love", "happy")
    val negativeWords = Set("bad", "terrible", "awful", "negative", "worst", "hate", "sad", "poor")
    
    // Tokenize and clean text
    val tokens = text.toLowerCase.split("\\W+")
    
    // Count positive and negative words
    val positiveCount = tokens.count(positiveWords.contains)
    val negativeCount = tokens.count(negativeWords.contains)
    
    // Calculate sentiment score between -1 and 1
    val totalWords = tokens.length.toDouble
    if (totalWords > 0) {
      (positiveCount - negativeCount) / totalWords
    } else {
      0.0
    }
  }
}

