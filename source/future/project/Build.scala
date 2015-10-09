import sbt._
import Keys._

object MyBuild extends Build {
  lazy val functional = ProjectRef(file("../functional"), "functional")
  
  lazy val future = Project("future", file(".")).dependsOn(functional)
}
