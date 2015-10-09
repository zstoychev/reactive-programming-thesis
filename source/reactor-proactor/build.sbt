name := "reactor-proactor"

version := "1.0"

scalaVersion := "2.11.7"

scalaSource in Compile := baseDirectory.value / "src"

scalaSource in Test := baseDirectory.value / "test"

resourceDirectory in Compile := baseDirectory.value / "resources"

libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.3.12"

scalacOptions ++= Seq("-feature")
