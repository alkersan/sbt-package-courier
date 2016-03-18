sbt-package-courier
===================
Plugin to deliver native packages to artifact storage

Requirements
------------
- [`sbt-native-packager`](https://github.com/sbt/sbt-native-packager) - latest stable release `1.0.6` linked transitively, but should work with higher versions too
- [`Artifactory Pro 4.+`](https://www.jfrog.com/artifactory/) with configured `apt` or `yum` repository

Usage
-----
#### Deb publishing
Assuming that `sbt-native-packager` is configured to build `deb` packages, Artifactory is hosted at `https://repo.acme.corp/artifactory` and `apt` repository is called `apt-repo` possible definitions of setting keys can be:

```scala
debianArtifactoryUrl in Debian := "https://repo.acme.corp/artifactory"
debianArtifactoryCredentials in Debian := Some(Credentials(Path.userHome / ".ivy2" / ".credentials"))
debianArtifactoryRepo in Debian := "apt-repo"
debianArtifactoryPath in Debian := s"pool/${packageName.value}"
debianArtifactoryDistribution in Debian := Seq("debian", "ubuntu")
debianArtifactoryComponent in Debian := Seq("main")
debianArtifactoryArchitecture in Debian := Seq("i386", "amd64")

publish in Debian <<= (debianArtifactoryPublish in Debian)
```

`Credentials` are optional, although by default Artifactory requires authentication for publishing.

Multiple values of `Distribution`, `Component`, `Architecture` can be specified, thus [`Matrix`](https://www.jfrog.com/confluence/display/RTF/Debian+Repositories#DebianRepositories-DeployingapackageusingMatrixParameters) publishing will be performed. By default `Architecture` is set to native-packager's `packageArchitecture`.

Then publishing is done with:
```shell
> sbt deb:publish
```

#### Rpm publishing
Assuming that `sbt-native-packager` is configured to build `rpm` packages, Artifactory is hosted at `https://repo.acme.corp/artifactory` and `yum` repository is called `yum-repo` possible definitions of setting keys can be:

```scala
rpmArtifactoryUrl in Rpm := "https://repo.acme.corp/artifactory"
rpmArtifactoryCredentials in Rpm := Some(Credentials(Path.userHome / ".ivy2" / ".credentials"))
rpmArtifactoryRepo in Rpm := "yum-repo"
rpmArtifactoryPath in Rpm := s"pool/${packageName.value}"

publish in Rpm <<= (rpmArtifactoryPublish in Rpm)
```

Be aware that correct nesting in `rpmArtifactoryPath` is critical for proper work of `yum` repo. Read more about [`repodata depth`](https://www.jfrog.com/confluence/display/RTF/YUM+Repositories#YUMRepositories-LocalRepositories)

Then publishing is done with:
```shell
> sbt rpm:publish
```

Future plans
------------

- trigger [index recalculation](https://www.jfrog.com/confluence/display/RTF/Artifactory+REST+API#ArtifactoryRESTAPI-CalculateDebianRepositoryMetadata) on publishing
- other artifact storages: [Aptly](http://www.aptly.info/), [Bintray](https://bintray.com), [PackageCloud](https://packagecloud.io/) to name a few.
