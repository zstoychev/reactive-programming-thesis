import sbt._
import Keys._

object MyBuild extends Build {
  lazy val functional = ProjectRef(file("../functional"), "functional")
  lazy val future = ProjectRef(file("../future"), "future")
  lazy val iteratee = ProjectRef(file("../iteratee"), "iteratee")
  lazy val reactorProactor = ProjectRef(file("../reactor-proactor"), "reactor-proactor")
  
  lazy val webServer = Project("web-server", file(".")).dependsOn(functional, future, iteratee, reactorProactor)
}
