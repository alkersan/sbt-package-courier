sbtPlugin := true

name := "sbt-package-courier"
organization := "io.sysa"
version := "0.1.0"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

scalaVersion in Global := "2.10.5"
scalacOptions in Compile ++= Seq("-deprecation", "-target:jvm-1.7")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6")


publishMavenStyle := false
bintrayOrganization := Some("sysa")
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish in ThisBuild := false
