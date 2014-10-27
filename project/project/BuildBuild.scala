import sbt.Keys._
import sbt._

/**
 *
 * @author mle
 */
object BuildBuild extends Build {
  // "build.sbt" goes here
  override lazy val settings = super.settings ++ Seq(
    scalaVersion := "2.10.4",
    resolvers ++= Seq(
      "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/",
      "Typesafe ivy releases" at "http://repo.typesafe.com/typesafe/ivy-releases/",
      "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"
    ),
    scalacOptions ++= Seq("-unchecked", "-deprecation")
  ) ++ sbtPlugins
  val mleGroup = "com.github.malliina"

  def sbtPlugins = Seq(
    "com.typesafe.play" % "sbt-plugin" % "2.3.4",
    mleGroup % "sbt-play" % "0.1.1",
    mleGroup %% "ssh-client" % "0.0.3",
    mleGroup %% "sbt-packager" % "1.2.2",
    mleGroup %% "sbt-play" % "0.1.0"
  ) map addSbtPlugin

  override lazy val projects = Seq(root)
  lazy val root = Project("plugins", file("."))
}
