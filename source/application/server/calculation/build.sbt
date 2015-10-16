import NativePackagerHelper._

name := "reactive-application-calculation"

libraryDependencies ++= Dependencies.calculation

enablePlugins(JavaServerAppPackaging)
mainClass in Compile := Some("reactivethesis.calculation.Main")

fork in run := true
