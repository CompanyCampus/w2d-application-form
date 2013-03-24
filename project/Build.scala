import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "w2d-appform"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      "play" %% "play-jdbc" % "2.1.0",
      "postgresql" % "postgresql" % "9.2-1002.jdbc4",
      "com.typesafe.slick" %% "slick" % "1.0.0",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "joda-time" % "joda-time" % "2.1",
      "org.joda" % "joda-convert" % "1.2"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")
    )

}
