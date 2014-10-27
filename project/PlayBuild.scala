import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbtplay.PlayProjects
import com.mle.ssh.{RemoteConfigReader, RootRemoteInfo, SSH}
import com.mle.util.Utils
import com.typesafe.sbt.SbtNativePackager
import play.PlayImport.PlayKeys
import sbt.Keys._
import sbt._
import sbtassembly.Plugin.AssemblyKeys._
import sbtassembly.Plugin._

import scala.concurrent.duration.DurationLong

object PlayBuild extends Build {

  lazy val p = PlayProjects.plainPlayProject("piweb").settings(commonSettings: _*)

  val remoteRun = taskKey[Unit]("Builds a local jar, transfers it to a remote machine and runs it remotely")
  val conf = taskKey[RootRemoteInfo]("The config")

  val mleGroup = "com.github.malliina"
  val commonSettings = remoteSettings ++ assemblySettings ++ SbtNativePackager.packagerSettings ++ LinuxPlugin.debianSettings ++ Seq(
    version := "0.0.4",
    scalaVersion := "2.11.2",
    retrieveManaged := false,
    fork in Test := true,
    libraryDependencies ++= Seq(
      mleGroup %% "pi-utils" % "0.1.5",
      mleGroup %% "play-base" % "0.1.2"),
    resolvers ++= Seq(
      "typesafe releases" at "http://repo.typesafe.com/typesafe/releases/",
      "Sonatype releases" at "https://oss.sonatype.org/content/repositories/releases/"),
    mainClass in assembly := Some("play.core.server.NettyServer"),
    fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value),
    mergeStrategy in assembly <<= (mergeStrategy in assembly)(old => {
      case "application.conf" =>
        MergeStrategy.concat
      case x if (x startsWith """org\apache\commons\logging""") || (x startsWith """play\core\server""") =>
        MergeStrategy.last
      case x if x startsWith """rx\""" =>
        MergeStrategy.first
      case "logger.xml" =>
        MergeStrategy.first
      case x =>
        old(x)
    })
  )
  def remoteSettings = Seq(
    conf := RemoteConfigReader.load,
    remoteRun := {
      val log = streams.value.log
      val file = assembly.value.toPath
      val remoteFileName = file.getFileName.toString
      val conf = RemoteConfigReader.load
      Utils.using(new SSH(conf.host, conf.port, conf.user, conf.key))(ssh => {
        log.info(s"Transferring: ${file.toAbsolutePath} to: $remoteFileName at: ${conf.user}@${conf.host}:${conf.port}...")
        ssh.scpAwait(file, remoteFileName)
        val runJarCommand = s"java -jar /home/${conf.user}/$remoteFileName"
        log.info(s"Transfer done. Running: $runJarCommand as root...")
        val response = ssh.execute("sudo -s -S", conf.rootPassword, runJarCommand, "exit")
        val sub = response.output.subscribe(line => log.info(line))
        val result = response.await(240.seconds)
        log.info(s"Exit: ${result.exitValue}")
        sub.unsubscribe()
      })
    }
  )
}