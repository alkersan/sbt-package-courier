sbtPlugin := true

name := "sbt-package-courier"
organization := "io.sysa"
version := "0.2.0"
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))

//scalaVersion in Global := "2.10.6"

scalaVersion := (CrossVersion partialVersion (sbtVersion in pluginCrossBuild).value match {
  case Some((0, 13)) => "2.10.6"
  case Some((1, _))  => "2.12.3"
  case _             => sys error s"Unhandled sbt version ${(sbtVersion in pluginCrossBuild).value}"
})

crossSbtVersions := Vector("0.13.16", "1.0.0")

//scalacOptions in Compile ++= Seq("-deprecation", "-target:jvm-1.7")

scalacOptions ++= (CrossVersion partialVersion (sbtVersion in pluginCrossBuild).value match {
  case Some((0, 13)) => Seq("-deprecation", "-target:jvm-1.7")
  case Some((1, _))  => Seq("-deprecation", "-target:jvm-1.8")
  case _             => sys error s"Unhandled sbt version ${(sbtVersion in pluginCrossBuild).value}"
})

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.2.2")


publishMavenStyle := false
bintrayOrganization := Some("sysa")
bintrayRepository := "sbt-plugins"
bintrayReleaseOnPublish in ThisBuild := false

// sbt dependend libraries
libraryDependencies ++= {
  (sbtVersion in pluginCrossBuild).value match {
    case v if v.startsWith("1.") =>
      Seq(
        "org.scala-sbt" %% "io" % "1.0.0"
      )
    case _ => Seq()
  }
}

// scala version depended libraries
libraryDependencies ++= {
  scalaBinaryVersion.value match {
    case "2.10" => Nil
    case _ =>
      Seq(
        "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.6",
        "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
      )
  }

}