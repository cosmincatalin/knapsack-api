logLevel := Level.Warn

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.5")

addSbtPlugin("com.timushev.sbt" % "sbt-updates" % "0.3.1")

addSbtPlugin("com.thesamet" % "sbt-protoc" % "0.99.11")

libraryDependencies += "com.trueaccord.scalapb" %% "compilerplugin" % "0.6.0"