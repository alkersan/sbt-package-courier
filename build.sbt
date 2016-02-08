sbtPlugin := true

name := "sbt-package-courier"
organization := "io.sysa"

scalaVersion in Global := "2.10.5"
scalacOptions in Compile ++= Seq("-deprecation", "-target:jvm-1.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6")