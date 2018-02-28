/*
scalafmt: {
  style = defaultWithAlign
  maxColumn = 150
  align.tokens = [
    { code = "=>", owner = "Case" }
    { code = "?", owner = "Case" }
    { code = "extends", owner = "Defn.(Class|Trait|Object)" }
    { code = "//", owner = ".*" }
    { code = "{", owner = "Template" }
    { code = "}", owner = "Template" }
    { code = ":=", owner = "Term.ApplyInfix" }
    { code = "++=", owner = "Term.ApplyInfix" }
    { code = "+=", owner = "Term.ApplyInfix" }
    { code = "%", owner = "Term.ApplyInfix" }
    { code = "%%", owner = "Term.ApplyInfix" }
    { code = "%%%", owner = "Term.ApplyInfix" }
    { code = "->", owner = "Term.ApplyInfix" }
    { code = "?", owner = "Term.ApplyInfix" }
    { code = "<-", owner = "Enumerator.Generator" }
    { code = "?", owner = "Enumerator.Generator" }
    { code = "=", owner = "(Enumerator.Val|Defn.(Va(l|r)|Def|Type))" }
  ]
}
 */

// Dependency versions
val akkaVersion                     = "2.5.9"
val akkaHttpVersion                 = "10.0.11"
val akkaPersistenceInMemVersion     = "2.5.1.1"
val akkaPersistenceCassandraVersion = "0.83"
val catsVersion                     = "1.0.1"
val circeVersion                    = "0.9.1"
val scalaTestVersion                = "3.0.5"
val shapelessVersion                = "2.3.3"
val jenaVersion                     = "3.6.0"
val commonsVersion                  = "0.7.7"
val sourcingVersion                 = "0.10.1"
val pureconfigVersion               = "0.8.0"

// Nexus dependency versions
val serviceVersion = "0.10.2"

// Dependency modules
lazy val akkaDistributed          = "com.typesafe.akka"   %% "akka-distributed-data"      % akkaVersion
lazy val akkaHttpCore             = "com.typesafe.akka"   %% "akka-http-core"             % akkaHttpVersion
lazy val akkaHttp                 = "com.typesafe.akka"   %% "akka-http"                  % akkaHttpVersion
lazy val akkaHttpTestKit          = "com.typesafe.akka"   %% "akka-http-testkit"          % akkaHttpVersion
lazy val akkaPersistenceCassandra = "com.typesafe.akka"   %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion
lazy val akkaPersistenceInMem     = "com.github.dnvriend" %% "akka-persistence-inmemory"  % akkaPersistenceInMemVersion

lazy val akkaStream   = "com.typesafe.akka"       %% "akka-stream"          % akkaVersion
lazy val akkaTestKit  = "com.typesafe.akka"       %% "akka-testkit"         % akkaVersion
lazy val catsCore     = "org.typelevel"           %% "cats-core"            % catsVersion
lazy val circeCore    = "io.circe"                %% "circe-core"           % circeVersion
lazy val circeJava8   = "io.circe"                %% "circe-java8"          % circeVersion
lazy val circeExtras  = "io.circe"                %% "circe-generic-extras" % circeVersion
lazy val commonsIam   = "ch.epfl.bluebrain.nexus" %% "iam"                  % commonsVersion
lazy val commonsTest  = "ch.epfl.bluebrain.nexus" %% "commons-test"         % commonsVersion
lazy val jenaArq      = "org.apache.jena"         % "jena-arq"              % jenaVersion
lazy val pureconfig   = "com.github.pureconfig"   %% "pureconfig"           % pureconfigVersion
lazy val shapeless    = "com.chuusai"             %% "shapeless"            % shapelessVersion
lazy val scalaTest    = "org.scalatest"           %% "scalatest"            % scalaTestVersion
lazy val slf4j        = "com.typesafe.akka"       %% "akka-slf4j"           % akkaVersion
lazy val sourcingMem  = "ch.epfl.bluebrain.nexus" %% "sourcing-mem"         % sourcingVersion
lazy val sourcingCore = "ch.epfl.bluebrain.nexus" %% "sourcing-core"        % sourcingVersion

// Nexus dependency modules
lazy val kamon         = "ch.epfl.bluebrain.nexus" %% "service-kamon"         % serviceVersion
lazy val serialization = "ch.epfl.bluebrain.nexus" %% "service-serialization" % serviceVersion

// Projects
lazy val ld = project
  .in(file("modules/ld"))
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoSettings,
    name       := "admin-ld",
    moduleName := "admin-ld",
    libraryDependencies ++= Seq(
      akkaHttpCore,
      catsCore,
      circeCore,
      commonsIam,
      jenaArq,
      shapeless,
      commonsTest % Test,
      scalaTest   % Test
    )
  )

lazy val core = project
  .in(file("modules/core"))
  .dependsOn(ld)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    buildInfoSettings,
    name       := "admin-core",
    moduleName := "admin-core",
    libraryDependencies ++= Seq(
      akkaPersistenceCassandra,
      akkaStream,
      circeExtras,
      circeJava8,
      commonsIam,
      pureconfig,
      serialization,
      sourcingCore,
      akkaDistributed      % Test,
      akkaPersistenceInMem % Test,
      akkaHttpTestKit      % Test,
      akkaTestKit          % Test,
      scalaTest            % Test,
      slf4j                % Test,
      sourcingMem          % Test
    )
  )

lazy val service = project
  .in(file("modules/service"))
  .enablePlugins(InstrumentationPlugin)
  .dependsOn(core)
  .settings(
    name       := "admin-service",
    moduleName := "admin-service",
    libraryDependencies ++= Seq(
      kamon,
      akkaHttp,
      scalaTest % Test,
    )
  )

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name       := "admin",
    moduleName := "admin"
  )
  .aggregate(ld, core, service)

/* ********************************************************
 ******************** Grouped Settings ********************
 **********************************************************/

lazy val noPublish = Seq(
  publishLocal    := {},
  publish         := {},
  publishArtifact := false,
)

lazy val buildInfoSettings =
  Seq(buildInfoKeys := Seq[BuildInfoKey](version), buildInfoPackage := "ch.epfl.bluebrain.nexus.admin.core.config")

inThisBuild(
  List(
    homepage := Some(url("https://github.com/BlueBrain/nexus-admin")),
    licenses := Seq("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.txt")),
    scmInfo  := Some(ScmInfo(url("https://github.com/BlueBrain/nexus-admin"), "scm:git:git@github.com:BlueBrain/nexus-admin.git")),
    developers := List(
      Developer("bogdanromanx", "Bogdan Roman", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("hygt", "Henry Genet", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("umbreak", "Didac Montero Mendez", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
      Developer("wwajerowicz", "Wojtek Wajerowicz", "noreply@epfl.ch", url("https://bluebrain.epfl.ch/")),
    ),
    // These are the sbt-release-early settings to configure
    releaseEarlyWith              := BintrayPublisher,
    releaseEarlyNoGpg             := true,
    releaseEarlyEnableSyncToMaven := false,
  ))
