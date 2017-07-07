name := "knapsack-api"

scalaVersion := "2.12.2"

mainClass in (Compile, run) := Some("com.cosminsanda.App")

libraryDependencies ++= {
    val log4jVersion = "2.8.2"
    val akkaHttp = "10.0.9"
    Seq(
        "org.apache.logging.log4j" % "log4j-api" % log4jVersion,
        "org.apache.logging.log4j" % "log4j-core" % log4jVersion,
        "org.apache.logging.log4j" % "log4j-slf4j-impl" % log4jVersion,
        "com.typesafe.akka" %% "akka-http" % akkaHttp,
        "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttp,
        "org.json4s" %% "json4s-native" % "3.5.2",
        "com.rabbitmq" % "amqp-client" % "4.1.1",
        "net.jodah" % "lyra" % "0.5.4",
        "org.scalikejdbc" %% "scalikejdbc" % "3.0.1",
        "mysql" % "mysql-connector-java" % "6.0.6",
        "net.codingwell" %%  "scala-guice" % "4.1.0"

    )
}

scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-encoding", "utf8")

assemblyJarName in assembly := name.value + ".jar"

PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
)