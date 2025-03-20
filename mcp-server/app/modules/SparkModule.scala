package modules

import com.google.inject.AbstractModule
import services.spark.{SparkService, DataProcessingService}
import play.api.{Configuration, Environment}

/**
 * Module for Spark-related services
 */
class SparkModule(environment: Environment, configuration: Configuration) extends AbstractModule {
  
  override def configure(): Unit = {
    bind(classOf[SparkService]).asEagerSingleton()
    bind(classOf[DataProcessingService])
  }
}

