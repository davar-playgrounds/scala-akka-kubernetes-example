import com.typesafe.sbt.packager.docker.DockerVersion

name := "scala-akka-kubernetes-example"

version := "0.3"

scalaVersion := "2.12.8"

libraryDependencies += "org.slf4j"          % "slf4j-api"     % "1.7.25"
libraryDependencies += "org.slf4j"          % "slf4j-log4j12" % "1.7.26"
libraryDependencies += "net.liftweb"       %% "lift-json"     % "3.3.0"
libraryDependencies += "com.typesafe.akka" %% "akka-actor"    % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-stream"   % "2.5.23"
libraryDependencies += "com.typesafe.akka" %% "akka-http"     % "10.1.8"
libraryDependencies += "org.scalatest"     %% "scalatest"     % "3.0.8"  % Test
libraryDependencies += "com.typesafe.akka" %% "akka-testkit"  % "2.5.23" % Test

enablePlugins(DockerPlugin,JavaAppPackaging)

// Docker image settings
dockerExposedPorts in Docker ++= Seq(9090)
dockerVersion in Docker := Some(DockerVersion(18, 9, 6, Some("ce")))
mappings in Docker := (mappings in Docker).value
dockerUpdateLatest in Docker := true
dockerBaseImage in Docker := "openjdk"
