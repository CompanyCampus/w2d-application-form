import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

    val appName         = "w2d-appform"
    val appVersion      = "1.0"

    val appDependencies = Seq(
      jdbc, filters,
      "com.typesafe" %% "play-plugins-mailer" % "2.2.0",
      "com.typesafe.slick" %% "slick" % "2.0.0",
      "com.typesafe.play" %% "play-slick" % "0.5.0.8",
      "org.slf4j" % "slf4j-nop" % "1.6.4",
      "joda-time" % "joda-time" % "2.1",
      "org.joda" % "joda-convert" % "1.2",
      "org.mindrot" % "jbcrypt" % "0.3m"
    )

    val main = play.Project(appName, appVersion, appDependencies).settings(
      scalacOptions ++= Seq("-feature", "-deprecation", "-unchecked")
    )

}
