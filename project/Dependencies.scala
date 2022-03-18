import sbt._

object Dependencies {

    val slf4jVersion = "1.7.20"
    val logbackVersion = "1.2.3"
    val scalatraVersion = "2.7.1"
    val jettyWebappVersion = "9.4.18.v20190429"
    val servletApiVersion = "3.1.0"
    val betterFilesVersion = "3.9.1"
    val jacksonVersion = "2.10.5"
    val s3mockVersion = "0.2.6"

    val cdr4sVersion = "3.0.9"
    val dartCommonsVersion = "3.0.30"
    val arangoDatastoreRepoVersion = "3.0.8"
    val dartAuthVersion = "3.1.11"
    val dartRestCommonsVersion = "3.0.4"

    val gsonOverrideVersion = "2.8.6"

    val logging = Seq( "org.slf4j" % "slf4j-api" % slf4jVersion,
                       "ch.qos.logback" % "logback-classic" % logbackVersion )

    val betterFiles = Seq( "com.github.pathikrit" %% "better-files" % betterFilesVersion )

    val scalatra = Seq( "org.scalatra" %% "scalatra-scalatest" % scalatraVersion % Test )

    val jackson = Seq( "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
                       "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" % jacksonVersion )

    val s3mock = Seq( "io.findify" %% "s3mock" % s3mockVersion % Test )

    val dartCommons = Seq( "com.twosixlabs.dart" %% "dart-aws" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-cli" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-utils" % dartCommonsVersion,
                           "com.twosixlabs.dart" %% "dart-test-base" % dartCommonsVersion % Test )

    val dartRest = Seq( "com.twosixlabs.dart.rest" %% "dart-scalatra-commons" % dartRestCommonsVersion )

    val dartAuth = Seq( "com.twosixlabs.dart.auth" %% "controllers" % dartAuthVersion,
                        "com.twosixlabs.dart.auth" %% "arrango-tenants" % dartAuthVersion )

    val jetty = Seq( "org.eclipse.jetty" % "jetty-webapp" % jettyWebappVersion,
                     "javax.servlet" % "javax.servlet-api" % servletApiVersion )

    val cdr4s = Seq( "com.twosixlabs.cdr4s" %% "cdr4s-core" % cdr4sVersion,
                     "com.twosixlabs.cdr4s" %% "cdr4s-dart-json" % cdr4sVersion )

    val arangoDatastoreRepo = Seq( "com.twosixlabs.dart" %% "dart-arangodb-datastore" % arangoDatastoreRepoVersion )

}
