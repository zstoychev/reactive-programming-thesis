name := "reactive-application-backend"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies ++= Dependencies.backend

fork in run := true
