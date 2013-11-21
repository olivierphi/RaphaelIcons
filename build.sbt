lazy val projectName = "RaphaelIcons"

name := projectName

// ---- base settings ----

lazy val commonSettings = Project.defaultSettings ++ Seq(
  version         := "1.0.0",
  organization    := "de.sciss",
  description     := "Icon set designed by Dmitry Baranovskiy",
  homepage        := Some(url("https://github.com/Sciss/" + projectName)),
  licenses        := Seq("LGPL v2.1+" -> url("http://www.gnu.org/licenses/lgpl-2.1.txt")),
  scalaVersion    := "2.10.3",
  retrieveManaged := true,
  scalacOptions  ++= Seq(
    "-no-specialization",
    // "-Xelide-below", "INFO", // elide debug logging!
    "-deprecation", "-unchecked", "-feature"
  )
)

// ---- sub-projects ----

lazy val root: Project = Project(
  id            = "root",
  base          = file("."),
  aggregate     = Seq(core, gen),
  settings      = Project.defaultSettings ++ Seq(
    packagedArtifacts := Map.empty
  )
)

lazy val java2DGenerator = TaskKey[Seq[File]]("java2d-generate", "Generate Icon Java2D source code")

lazy val core = Project(
  id        = "raphael-icons",
  base      = file("core"),
  settings  = commonSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.scala-lang" % "scala-swing" % scalaVersion.value % "test"
    ),
    sourceGenerators in Compile <+= (java2DGenerator in Compile),
    java2DGenerator in Compile <<=
      (resourceDirectory   in Compile in gen,
       sourceManaged       in Compile,
       dependencyClasspath in Runtime in gen,
       streams) map {
        (spec, src, cp, st) => runJava2DGenerator(spec, src, cp.files, st.log)
      },
    // ---- publishing ----
    // publishArtifact in (Compile, packageDoc) := false,
    // publishArtifact in (Compile, packageSrc) := false,
    publishArtifact in Test := false,
    publishMavenStyle := true,
    publishTo :=
      Some(if (version.value endsWith "-SNAPSHOT")
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
      else
        "Sonatype Releases"  at "https://oss.sonatype.org/service/local/staging/deploy/maven2"
      ),
    pomIncludeRepository := { _ => false },
    pomExtra := { val n = projectName
<scm>
  <url>git@github.com:Sciss/{n}.git</url>
  <connection>scm:git:git@github.com:Sciss/{n}.git</connection>
</scm>
<developers>
  <developer>
    <id>sciss</id>
    <name>Hanns Holger Rutz</name>
    <url>http://www.sciss.de</url>
  </developer>
  <developer>
    <id>DmitryBaranovskiy</id>
    <name>Dmitry Baranovskiy</name>
    <url>http://dmitry.baranovskiy.com/</url>
  </developer>
</developers>
    }
  )
)

def runJava2DGenerator(specDir: File, outputDir: File, cp: Seq[File], log: Logger): Seq[File] = {
  val outDir2     = outputDir / /* "java" / */ "de" / "sciss" / "icons" / "raphael"
  outDir2.mkdirs()
  val outFile     = outDir2 / "Shapes.java"
  val mainClass   = "de.sciss.icons.raphael.Generate"
  val os          = new java.io.FileOutputStream(outFile)
  log.info("Generating Java2D source code...")
  try {
    val outs  = CustomOutput(os)  // Generate.scala writes the class source file to standard out
    val p     = new Fork.ForkScala(mainClass).fork(javaHome = None, jvmOptions = Nil, scalaJars = cp,
      arguments = Nil, workingDirectory = None,
      connectInput = false, outputStrategy = outs)
    val res = p.exitValue()
    if (res != 0) {
      sys.error("Java2D class file generator failed with exit code " + res)
    }
  } finally {
    os.close()
  }
  val sources = outFile :: Nil
  sources
}

lazy val gen = Project(
  id        = "generate",
  base      = file("generate"),
  settings  = commonSettings ++ Seq(
    libraryDependencies ++= Seq(
      "org.apache.xmlgraphics" % "batik-parser" % "1.7"
    )
  )
)
