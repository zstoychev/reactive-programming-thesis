import NativePackagerHelper._

name := "reactive-application-backend"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies ++= Dependencies.backend

enablePlugins(JavaServerAppPackaging)
mainClass in Compile := Some("reactivethesis.backend.Main")

fork in run := true
