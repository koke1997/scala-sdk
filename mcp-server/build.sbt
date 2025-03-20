name := "mcp-server"
organization := "com.example"
version := "1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala)

scalaVersion := "2.13.12"

// Add proper resolvers for Apache Pekko
resolvers ++= Seq(
  "Apache Repository" at "https://repository.apache.org/content/groups/public/",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

// Define versions
val pekkoVersion = "1.0.2"
val pekkoHttpVersion = "1.0.0" // Different version for HTTP
val pekkoConnectorsVersion = "1.0.0" // For Kafka connectors

libraryDependencies ++= Seq(
  guice,
  // Core Pekko dependencies - these exist
  "org.apache.pekko" %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko" %% "pekko-stream" % pekkoVersion,
  "org.apache.pekko" %% "pekko-slf4j" % pekkoVersion,
  
  // Pekko HTTP for API calls
  "org.apache.pekko" %% "pekko-http" % pekkoHttpVersion,
  
  // Pekko Kafka integration - using connectors
  "org.apache.pekko" %% "pekko-connectors-kafka" % pekkoConnectorsVersion,
  
  // Basic serialization
  "org.apache.pekko" %% "pekko-serialization-jackson" % pekkoVersion,
  
  // For testing
  "org.apache.pekko" %% "pekko-stream-testkit" % pekkoVersion % Test,
  "org.scalatestplus.play" %% "scalatestplus-play" % "6.0.0" % Test,
  
  // ZIO
  "dev.zio" %% "zio" % "2.0.15",
  "dev.zio" %% "zio-interop-cats" % "23.0.0.8",
  
  // Play Framework libraries
  "org.playframework" %% "play-json" % "3.0.1",
  "org.playframework" %% "play-ahc-ws" % "3.0.0",
  
  // Cats libraries
  "org.typelevel" %% "cats-core" % "2.10.0",
  "org.typelevel" %% "cats-effect" % "3.5.0",
  
  // Add logback for better logging
  "ch.qos.logback" % "logback-classic" % "1.4.11",

  //Add async-http-client
  "org.asynchttpclient" % "async-http-client" % "3.0.1",

  //Add Apache Spark
  "org.apache.spark" %% "spark-core" % "3.4.1",
  "org.apache.spark" %% "spark-sql" % "3.4.1",
  "org.apache.spark" %% "spark-mllib" % "3.4.1",
  
  // Add AsyncHttpClient directly to ensure compatibility
  "org.asynchttpclient" % "async-http-client" % "2.12.3",
  
  // Add Apache Commons CSV for data processing
  "org.apache.commons" % "commons-csv" % "1.10.0"
  
)

// Add scalac options
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-unchecked",
  "-Xlint:-unused,_"
)

// Code coverage configuration
coverageEnabled := false
coverageExcludedPackages := "<empty>;Reverse.*;router\\.*"

// Avoid duplicate protobuf-v3-testkit dependencies
dependencyOverrides ++= Seq(
  "org.apache.pekko" %% "pekko-serialization-protobuf-v3-testkit" % "1.0.2" % Test,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
)


// Add this after your libraryDependencies section
libraryDependencySchemes += "org.scala-lang.modules" %% "scala-parser-combinators" % "semver-spec"

