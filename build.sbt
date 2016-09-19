name := "sv-play"

version := "1.0"

lazy val `sv-play` = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "mysql" % "mysql-connector-java" % "5.1.34",
  "org.postgresql" % "postgresql" % "9.4-1201-jdbc41",
  "org.scalikejdbc" %% "scalikejdbc"       % "2.2.2",
  "org.scalikejdbc" %% "scalikejdbc-config"  % "2.2.2",
  "com.typesafe.play" %% "anorm" % "2.4.0",
  "org.apache.pdfbox" % "pdfbox" % "2.0.2",
  "org.apache.pdfbox" % "pdfbox-tools" % "2.0.2",
  "com.github.tototoshi" %% "scala-csv" % "1.2.2",
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.0" % "test",
  "org.apache.httpcomponents" % "httpclient" % "4.5",
  "com.typesafe" % "config" % "1.2.1",
  "org.languagetool" % "language-en" % "3.3",
  "org.codehaus.plexus" % "plexus-utils" % "3.0.22",
  "com.sun.mail" % "javax.mail" % "1.5.5",
  "org.scalanlp" %% "breeze" % "0.12",
  "org.scalanlp" %% "breeze-natives" % "0.12",
  "pdeboer" %% "pplib" % "0.1-SNAPSHOT"
)

libraryDependencies ++= Seq( jdbc , cache , ws , evolutions )

libraryDependencies ~= { _.map(_.exclude("org.slf4j", "slf4j-log4j12")) }

resolvers += Resolver.file("Local repo", file("custom_lib"))(Resolver.ivyStylePatterns)

resolvers ++= Seq(
  "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
  "Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/"
)

unmanagedResourceDirectories in Test <+=  baseDirectory ( _ /"target/web/public/test" )