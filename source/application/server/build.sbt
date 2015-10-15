name := "reactive-application"

lazy val commonSettings = Seq(
  version := "1.0.0"
)

scalaVersion in Global := "2.11.7"

lazy val root = (project in file(".")).settings(commonSettings: _*).settings(aggregate in update := false).aggregate(
  common, frontend, backend, calculation)

lazy val common = project.settings(commonSettings: _*)
lazy val frontend = project.settings(commonSettings: _*).dependsOn(common).enablePlugins(PlayScala)
lazy val backend = project.settings(commonSettings: _*).dependsOn(common)
lazy val calculation = project.settings(commonSettings: _*).dependsOn(common)
