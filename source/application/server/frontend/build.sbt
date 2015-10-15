name := "reactive-application-frontend"

//lazy val frontend = (project in file("."))
resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

libraryDependencies ++= Dependencies.frontend ++ Seq(
  filters,
  cache,
  ws
)

routesGenerator := InjectedRoutesGenerator
