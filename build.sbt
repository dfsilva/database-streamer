
name := "database-streamer"

version := "0.1"

scalaVersion := "2.13.4"

enablePlugins(DockerPlugin)

resolvers += Resolver.mavenLocal

libraryDependencies ++= {
  val akka = "com.typesafe.akka"
  val akkaV = "2.6.18"
  val akkaHttpV = "10.2.6"
  val circeVersion = "0.14.1"
  val slickVersion = "3.3.3"
  Seq(
    akka %% "akka-actor-typed" % akkaV,
    akka %% "akka-stream-typed" % akkaV,
    akka %% "akka-cluster-tools" % akkaV,
    akka %% "akka-cluster-sharding-typed" % akkaV,
    akka %% "akka-serialization-jackson" % akkaV,
    akka %% "akka-persistence-typed" % akkaV,
    akka %% "akka-persistence-query" % akkaV,
    "com.lightbend.akka" %% "akka-persistence-jdbc" % "5.0.4",
    "com.typesafe.slick" %% "slick" % slickVersion,
    "com.typesafe.slick" %% "slick-hikaricp" % slickVersion,
    "com.github.tminglei" %% "slick-pg" % "0.19.7",
    "com.github.tminglei" %% "slick-pg_circe-json" % "0.19.7",

    "org.flywaydb" % "flyway-core" % "7.15.0",
    "org.postgresql" % "postgresql" % "42.2.24",

    akka %% "akka-slf4j" % akkaV,
    akka %% "akka-http" % akkaHttpV,

    "ch.qos.logback" % "logback-classic" % "1.2.3",

    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-generic-extras" % circeVersion,

    "de.heikoseeberger" %% "akka-http-circe" % "1.38.2",

    "br.com.diego.silva" % "nats-stream-sdk" % "1.0.3-SNAPSHOT",
  )
}

scalacOptions += "-Ymacro-annotations"

assemblyJarName in assembly := "database-streamer.jar"

assemblyMergeStrategy in assembly := {
  case PathList("reference.conf") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) =>
    (xs map {_.toLowerCase}) match {
      case ("manifest.mf" :: Nil) | ("index.list" :: Nil) | ("dependencies" :: Nil) =>
        MergeStrategy.discard
      case ps @ (x :: xs) if ps.last.endsWith(".sf") || ps.last.endsWith(".dsa") =>
        MergeStrategy.discard
      case "plexus" :: xs =>
        MergeStrategy.discard
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case ("spring.schemas" :: Nil) | ("spring.handlers" :: Nil) =>
        MergeStrategy.filterDistinctLines
      case _ => MergeStrategy.discard
    }
  case _ => MergeStrategy.first
}

mainClass in assembly := Some("br.com.diegosilva.database.streamer.Main")

dockerfile in docker := {
  val artifact: File = assembly.value
  val artifactTargetPath = s"/app/${artifact.name}"

  new Dockerfile {
    from("openjdk:11-jre")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-jar", artifactTargetPath)
  }
}

buildOptions in docker := BuildOptions(cache = false)
