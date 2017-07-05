name := "knapsack-api"

scalaVersion := "2.12.2"

mainClass in (Compile, run) := Some("com.cosminsanda.App")

libraryDependencies ++= {
    val log4jVersion = "2.8.2"
    val akkaHttp = "10.0.9"
    val json4sNative = "3.5.2"
    Seq(
        "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
        "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttp,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttp,
        "org.json4s" %% "json4s-native" % json4sNative
    )
}

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

assemblyJarName in assembly := name.value + ".jar"
