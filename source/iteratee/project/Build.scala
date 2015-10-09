import sbt._
import Keys._

object MyBuild extends Build {
  lazy val functional = ProjectRef(file("../functional"), "functional")
  lazy val future = ProjectRef(file("../future"), "future")
  
  lazy val iteratee = Project("iteratee", file(".")).dependsOn(functional, future)
}
