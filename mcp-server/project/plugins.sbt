// Add proper resolvers for Apache Pekko
resolvers += "Apache Repository" at "https://repository.apache.org/content/groups/public/"
resolvers += "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"

// The Play plugin
addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.0")

// Formatting and style
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// Code coverage
addSbtPlugin("org.scoverage" % "sbt-scoverage" % "2.0.8")

// Optional: For better development experience
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.16")
