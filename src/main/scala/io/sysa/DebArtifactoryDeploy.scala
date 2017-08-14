package io.sysa

import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.debian.DebianPlugin

import java.io.FileInputStream
import java.net.HttpURLConnection
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter.printBase64Binary


trait DebArtifactoryDeployKeys {
  val debianArtifactoryUrl = SettingKey[String]("debian-artifactory-url", " Url of Atrifactory server")
  val debianArtifactoryRepo = SettingKey[String]("debian-artifactory-repo", "Name of Artifactory repository with deb layout to publish artifacts to")
  val debianArtifactoryCredentials = TaskKey[Option[Credentials]]("debian-artifactory-credentials", "Credentials with permissions to publish to Artifactory server")

  val debianArtifactoryPath = SettingKey[String]("debian-artifactory-path", "Path in repository where the package should be stored (e.g. pool)")
  val debianArtifactoryPublishName = TaskKey[String]("debian-artifactory-package-name", "Final name package will be published with")
  val debianArtifactoryDistribution = SettingKey[Seq[String]]("debian-artifactory-distribution", "The value to assign to the deb.distribution property used to specify the Debian package distribution")
  val debianArtifactoryComponent = SettingKey[Seq[String]]("debian-artifactory-component", "The value to assign to the deb.component property used to specify the Debian package component name")
  val debianArtifactoryArchitecture = SettingKey[Seq[String]]("debian-artifactory-architecture", "The value to assign to the deb.architecture property used to specify the Debian package architecture")

  val debianArtifactoryTargetPath = TaskKey[String]("debian-artifactory-target-path", "Construct Atrifactory specific url path used for publishing")
  val debianArtifactoryPublish = TaskKey[Unit]("debian-artifactory-publish", "Publish debian package to Artifactory")
}

object DebArtifactoryDeployPlugin extends AutoPlugin {

  override def requires = DebianPlugin

  override def trigger = NoTrigger

  object autoImport extends DebArtifactoryDeployKeys

  import autoImport._

  override def projectSettings = inConfig(Debian)(Seq(
    debianArtifactoryPublishName := (packageName in Debian).value + "_" + version.value + "_" + (packageArchitecture in Debian).value + ".deb",
    debianArtifactoryArchitecture := Seq((packageArchitecture in Debian).value),
    debianArtifactoryTargetPath := makeTargetPath(debianArtifactoryPublishName.value, debianArtifactoryPath.value, debianArtifactoryDistribution.value, debianArtifactoryComponent.value, debianArtifactoryArchitecture.value),
    debianArtifactoryPublish := publishToArtifactory(debianArtifactoryUrl.value, debianArtifactoryRepo.value, debianArtifactoryTargetPath.value, (packageBin in Debian).value, debianArtifactoryCredentials.value, streams.value)
  ))

  private def makeTargetPath(name: String, repoPath: String, distribution: Seq[String], component: Seq[String], arch: Seq[String]): String = {
    val dists = distribution.map(d => s"deb.distribution=$d")
    val comps = component.map(c => s"deb.component=$c")
    val archs = arch.map(a => s"deb.architecture=$a")
    (s"$repoPath/$name" +: dists ++: comps ++: archs).mkString(";")
  }

  private def publishToArtifactory(artUrl: String, repo: String, targetPath: String, pkg: File, creds: Option[Credentials], streams: TaskStreams): Unit = {
    val putTo = uri(s"$artUrl/$repo/$targetPath").normalize().toURL

    val connection = putTo.openConnection().asInstanceOf[HttpURLConnection]
    connection.setRequestMethod("PUT")
    connection.setDoOutput(true)

    Credentials.forHost(creds.toSeq, putTo.getHost) match {
      case None if creds.isEmpty =>
        streams.log.info(s"Artifactory credentials weren't supplied, proceeding without authentication")
      case None if creds.isDefined =>
        streams.log.warn(s"Couldn't find corresponding credentials for Artifactory host ${putTo.getHost}, proceeding without authentication")
      case Some(dc) =>
        streams.log.info(s"Found credentials for Artifactory host ${putTo.getHost}, proceeding with Basic authentication")
        val userpass = s"${dc.userName}:${dc.passwd}"
        val authHeader = "Basic " + printBase64Binary(userpass.getBytes)
        connection.setRequestProperty("Authorization", authHeader)
    }

    streams.log.info(s"Calculating checksums")
    connection.setRequestProperty("X-Checksum-Md5", pkgChecksum("MD5", pkg))
    connection.setRequestProperty("X-Checksum-Sha1", pkgChecksum("SHA-1", pkg))

    streams.log.info(s"Publishing package ${pkg.name} to $putTo")

    connection.connect()
    IO.transfer(pkg, connection.getOutputStream)
    connection.getOutputStream.flush()
    connection.getOutputStream.close()

    if (connection.getResponseCode == HttpURLConnection.HTTP_OK || connection.getResponseCode == HttpURLConnection.HTTP_CREATED) {
      val response = IO.readStream(connection.getInputStream)
      connection.getInputStream.close()
      streams.log.info(s"Package published:\n$response")
    } else {
      streams.log.error(s"Publish failed: ${connection.getResponseMessage}")
    }
    connection.disconnect()
  }

  private def pkgChecksum(algo: String, pkg: File): String = {
    val md = MessageDigest.getInstance(algo)
    val input = new FileInputStream(pkg)
    val buffer = new Array[Byte](1024)
    Stream.continually(input.read(buffer)).takeWhile(_ != -1).foreach(md.update(buffer, 0, _))
    md.digest.map("%02X" format _).mkString
  }
}
