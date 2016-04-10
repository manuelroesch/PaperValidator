name := "sv-play"

version := "1.0"

lazy val `sv-play` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.scalikejdbc" %% "scalikejdbc"       % "2.2.2",
  "com.h2database"  %  "h2"                % "1.4.184",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.2.2",
  "com.typesafe.play" %% "anorm" % "2.4.0",
  "org.apache.pdfbox" % "pdfbox" % "1.8.10",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "com.github.tototoshi" %% "scala-csv" % "1.2.2",
  "junit" % "junit" % "4.8.1" % "test",
  "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test",
  "com.novocode" % "junit-interface" % "0.8" % "test->default",
  "org.apache.httpcomponents" % "httpclient" % "4.5",
  "com.typesafe" % "config" % "1.2.1",
  "org.codehaus.plexus" % "plexus-utils" % "3.0.22",
  "pdeboer" %% "pplib" % "0.1-SNAPSHOT"
)

libraryDependencies ++= Seq( jdbc , cache , ws   , specs2 % Test , evolutions )

resolvers += Resolver.file("Local repo", file("custom_lib"))(Resolver.ivyStylePatterns)

resolvers += "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases"

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )

