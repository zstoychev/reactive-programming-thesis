import sbt._

object Dependencies {
  val common = Seq(
    "com.typesafe.akka" %% "akka-actor" % "2.4.0",
    "com.typesafe.akka" %% "akka-cluster" % "2.4.0",
    "com.typesafe.akka" %% "akka-slf4j" % "2.4.0",
    "com.typesafe.akka" % "akka-cluster-metrics_2.11" % "2.4.0",
    "com.typesafe.akka" %% "akka-cluster-sharding" % "2.4.0",
    "com.typesafe.play" %% "play" % "2.4.3",
    "com.typesafe.akka" %% "akka-persistence" % "2.4.0",
    "com.typesafe.akka" %% "akka-cluster-tools" % "2.4.0"
  )
  
  val frontend = common
  val calculation = common
  val backend = common ++ Seq(
    "com.github.krasserm" %% "akka-persistence-cassandra" % "0.4"
  )
}
