import Dependencies._
import sbt._

organization in ThisBuild := "com.twosixlabs"
name := "cdr-retrieval"
scalaVersion in ThisBuild := "2.12.7"

resolvers in ThisBuild ++= Seq( "Maven Central" at "https://repo1.maven.org/maven2/",
                                "JCenter" at "https://jcenter.bintray.com",
                                "Local Ivy Repository" at s"file://${System.getProperty( "user.home" )}/.ivy2/local/default" )

lazy val root = ( project in file( "." ) ).settings( libraryDependencies ++= cdr4s ++
                                                                             arangoDatastoreRepo ++
                                                                             dartCommons ++
                                                                             dartRest ++
                                                                             dartAuth ++
                                                                             jetty ++
                                                                             scalatra ++
                                                                             logging ++
                                                                             jackson ++
                                                                             betterFiles ++
                                                                             s3mock,
                                                     excludeDependencies ++= Seq( ExclusionRule( "org.slf4j", "slf4j-log4j12" ),
                                                                                  ExclusionRule( "javax.ws.rs", "javax.ws.rs-api" ),
                                                                                  ExclusionRule( "javax.servlet", "servlet-api" ),
                                                                                  ExclusionRule( "org.slf4j", "slf4j-log4j12" ),
                                                                                  ExclusionRule( "org.slf4j", "log4j-over-slf4j" ),
                                                                                  ExclusionRule( "log4j", "log4j" ),
                                                                                  ExclusionRule( "org.apache.logging.log4j", "log4j-core" ) ),
                                                     dependencyOverrides ++= Seq( "javax.servlet" % "javax.servlet-api" % servletApiVersion,
                                                                                  "com.google.code.gson" % "gson" % gsonOverrideVersion,
                                                                                  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
                                                                                  "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
                                                                                  "com.arangodb" %% "velocypack-module-scala" % "1.2.0",
                                                                                  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion) )

mainClass in(Compile, run) := Some( "Main" )

enablePlugins( JavaAppPackaging )

// don't run tests when build the fat jar, use sbt test instead for that (takes too long when building the image)
test in assembly := {}

parallelExecution in Test := false

assemblyMergeStrategy in assembly := {
    case PathList( "META-INF", "MANIFEST.MF" ) => MergeStrategy.discard
    case PathList( "reference.conf" ) => MergeStrategy.concat
    case x => MergeStrategy.last
}

