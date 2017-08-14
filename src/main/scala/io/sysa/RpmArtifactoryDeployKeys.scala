package io.sysa

import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.Keys._
import com.typesafe.sbt.packager.rpm.RpmPlugin

import java.io.FileInputStream
import java.net.HttpURLConnection
import java.security.MessageDigest
import javax.xml.bind.DatatypeConverter.printBase64Binary


trait RpmArtifactoryDeployKeys {
  val rpmArtifactoryUrl = SettingKey[String]("rpm-artifactory-url", " Url of Atrifactory server")
  val rpmArtifactoryRepo = SettingKey[String]("rpm-artifactory-repo", "Name of Artifactory repository with YUM layout to publish artifacts to")
  val rpmArtifactoryCredentials = TaskKey[Option[Credentials]]("rpm-artifactory-credentials", "Credentials with permissions to publish to Artifactory server")

  val rpmArtifactoryPath = SettingKey[String]("rpm-artifactory-path", "Path in repository where the package should be stored (e.g. pool)")
  val rpmArtifactoryPublishName = TaskKey[String]("rpm-artifactory-publish-name", "Final name package will be published with")

  val rpmArtifactoryRecalcMetadata = TaskKey[Boolean]("rpm-artifactory-recalc-metadata", "Trigger or not YUM metadata recalculation after publishing")

  val rpmArtifactoryPublish = TaskKey[Unit]("rpm-artifactory-publish", "Publish rpm package to Artifactory")
}

object RpmArtifactoryDeployPlugin extends AutoPlugin {
  override def requires = RpmPlugin

  override def trigger = NoTrigger

  object autoImport extends RpmArtifactoryDeployKeys

  import autoImport._

  override def projectSettings = inConfig(Rpm)(Seq(
    rpmArtifactoryPublishName := {
      val spec = (rpmSpecConfig in Rpm).value
      "%s-%s-%s.%s.rpm" format (spec.meta.name, spec.meta.version, spec.meta.release, spec.meta.arch)
    },

    rpmArtifactoryRecalcMetadata := true,

    rpmArtifactoryPublish := publishToArtifactory(rpmArtifactoryUrl.value, rpmArtifactoryRepo.value, rpmArtifactoryPath.value, rpmArtifactoryPublishName.value, (packageBin in Rpm).value, rpmArtifactoryCredentials.value, rpmArtifactoryRecalcMetadata.value, streams.value)
  ))

  private def publishToArtifactory(artUrl: String, repo: String, path: String, name: String, pkg: File, creds: Option[Credentials], recalcMetadata: Boolean, streams: TaskStreams): Unit = {
    val putTo = uri(s"$artUrl/$repo/$path/$name").normalize().toURL

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

    streams.log.info(s"Publishing package $name to $putTo")

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