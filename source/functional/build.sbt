name := "functional"

version := "1.0"

scalaVersion := "2.11.7"

scalaSource in Compile := baseDirectory.value / "src"

libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.1.3"
