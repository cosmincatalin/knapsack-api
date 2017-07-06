logLevel := Level.Warn

addSbtPlugin("io.spray" % "sbt-revolver" % "0.8.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.4")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.0")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.8")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.5.47"