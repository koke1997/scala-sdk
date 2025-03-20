package core.config

import javax.inject._
import play.api.Configuration
import scala.concurrent.duration._

/**
 * Type-safe configuration for HTTP settings
 */
@Singleton
class HttpConfig @Inject()(config: Configuration) {
  val connectionTimeout: FiniteDuration = config.getOptional[Int]("http.timeout.connection")
    .map(_.seconds).getOrElse(60.seconds)
    
  val requestTimeout: FiniteDuration = config.getOptional[Int]("http.timeout.request")
    .map(_.seconds).getOrElse(300.seconds)
    
  val idleTimeout: FiniteDuration = config.getOptional[Int]("http.timeout.idle")
    .map(_.seconds).getOrElse(300.seconds)
}

