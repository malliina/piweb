import com.mle.sbt.unix.LinuxPlugin
import com.mle.sbtplay.PlayProjects
import com.typesafe.sbt.SbtNativePackager
import sbt.Keys._
import sbt._

object PlayBuild extends Build {

  lazy val p = PlayProjects.plainPlayProject("piweb").settings(commonSettings: _*)
  val mleGroup = "com.github.malliina"
  val commonSettings = SbtNativePackager.packagerSettings ++ LinuxPlugin.debianSettings ++ Seq(
    scalaVersion := "2.11.2",
    retrieveManaged := false,
    fork in Test := true,
    libraryDependencies ++= Seq(
      mleGroup %% "pi-utils" % "0.1.2",
      mleGroup %% "play-base" % "0.1.2")
  )
}