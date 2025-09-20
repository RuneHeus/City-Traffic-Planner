import scala.sys.process.*

resolvers += "Sonatype releases" at "https://oss.sonatype.org/content/repositories/snapshots"

//fork a process when running so that javafx doesn't crash
fork := true

//include ./conf in classpath
Compile / unmanagedResourceDirectories += baseDirectory.value / "conf"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-new-syntax",
  "-indent"
)



lazy val copyright = "(c) 2016 Nicola Justus"
lazy val licenseName = "MPL V2.0 <https://mozilla.org/MPL/2.0>"

lazy val copyRscs =
  taskKey[Unit]("Copies needed resources to resource-directory.")

lazy val rscFiles =
  settingKey[Seq[File]]("The files that get copied with copyRscs task")

lazy val rscCopyTarget =
  settingKey[File]("The target directory for copyRscs task")

lazy val moveConfigDir = settingKey[File]("The config directory of move")

lazy val cleanConfig = taskKey[Unit]("Cleans user's config directory of move")

rscFiles := Seq(baseDirectory.value / "LICENSE")

moveConfigDir := new File(System.getProperty("user.home") + "/.move")

rscCopyTarget := (Compile / classDirectory).value

copyRscs := rscFiles.value.map { file =>
  IO.copyFile(file, rscCopyTarget.value / file.getName)
}

cleanConfig := IO.delete(moveConfigDir.value)

//append copyRscs-task to compile-task
compile := ((Compile / compile) dependsOn copyRscs).value


// The openCoverageReport task
// This task opens the scoverage report (if available) in the default web browser.
// It detects the operating system and uses the appropriate command to launch the browser.
//
// The task performs the following steps:
// 1. Determines the path of the scoverage report: `<project_target>/scoverage-report/index.html`.
// 2. Checks if the report exists at the expected location.
// 3. Based on the operating system, it uses the corresponding command:
//    - `cmd /c start` for Windows
//    - `open` for macOS
//    - `xdg-open` for Linux/Unix
// 4. If the operating system is unsupported, it prints an error message.
// 5. If the report file is not found, it informs the user via a message.
lazy val openCoverageReport = taskKey[Unit]("Opens the coverage report in the default browser.")

openCoverageReport := {
  val reportFile = crossTarget.value / "scoverage-report" / "index.html"

  if (reportFile.exists()) {
    val reportPath = reportFile.getAbsolutePath
    println(s"Opening scoverage report: $reportPath")

    val os = System.getProperty("os.name").toLowerCase

    if (os.contains("win")) {
      // Windows
      Process(Seq("cmd", "/c", "start", reportPath)).!
    } else if (os.contains("mac")) {
      // macOS
      Process(Seq("open", reportPath)).!
    } else if (os.contains("nix") || os.contains("nux") || os.contains("bsd")) {
      // Linux/Unix
      Process(Seq("xdg-open", reportPath)).!
    } else {
      println(s"Unsupported operating system: $os. Cannot open the report.")
    }
  } else {
    println(s"Coverage report not found: ${reportFile.getAbsolutePath}")
  }
}

lazy val root = (project in file("."))
  .settings(
    organization := "de.thm.mote",
    name := "Move",
    version := "0.7.1",
    scalaVersion := "3.3.1",
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
      "org.scala-lang.modules" %% "scala-xml" % "2.3.0",
      "org.scalatest" %% "scalatest" % "3.2.18" % "test",
      "org.reactfx" % "reactfx" % "2.0-SNAPSHOT",
      "com.lihaoyi" %% "upickle" % "3.3.0",
      "org.scala-lang" %% "toolkit" % "0.4.0",
      "org.mockito" % "mockito-core" % "5.11.0" % "test",
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % "test",


      // Test dependencies

      "org.scalatestplus" %% "mockito-5-12" % "3.2.19.0" % "test",

      "org.testfx" % "testfx-core" % "4.0.17" % "test",
      "junit" % "junit" % "4.13.2" % "test",
      "com.github.sbt" % "junit-interface" % "0.13.3" % "test",
      "org.junit.jupiter" % "junit-jupiter-api" % "5.10.0" % Test,
      "org.junit.jupiter" % "junit-jupiter-engine" % "5.10.0" % Test,
      "org.testfx" % "testfx-junit" % "4.0.18" % "test",
      "org.testfx" % "openjfx-monocle" % "21.0.2" % "test",
      "org.mockito" % "mockito-core" % "5.11.0" % "test",
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % "test",
      "org.powermock" % "powermock-module-junit4" % "2.0.9" % Test,
      "org.powermock" % "powermock-api-mockito2" % "2.0.9" % Test
    ),
    libraryDependencies ++= {
      val javaFxVersion = "23"

      Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
        .map(m => "org.openjfx" % s"javafx-$m" % javaFxVersion)
    },
    Compile / mainClass := Some("de.thm.move.MoveApp")
  )
