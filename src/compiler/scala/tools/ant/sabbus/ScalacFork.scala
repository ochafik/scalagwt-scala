/*                     __                                               *\
**     ________ ___   / /  ___     Scala Ant Tasks                      **
**    / __/ __// _ | / /  / _ |    (c) 2005-2010, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */


package scala.tools.ant
package sabbus

import java.io.File
import java.io.FileWriter
import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.{ MatchingTask, Java }
import org.apache.tools.ant.util.{ GlobPatternMapper, SourceFileScanner }
import scala.tools.nsc.io
import scala.tools.nsc.util.ScalaClassLoader

class ScalacFork extends MatchingTask with ScalacShared with TaskArgs {
  private def originOfThis: String =
    ScalaClassLoader.originOfClass(classOf[ScalacFork]) map (_.toString) getOrElse "<unknown>"

  def setSrcdir(input: File) {
    sourceDir = Some(input)
  }

  def setFailOnError(input: Boolean) {
    failOnError = input
  }

  def setTimeout(input: Long) {
    timeout = Some(input)
  }

  def setJvmArgs(input: String) {
    jvmArgs = Some(input)
  }

  def setArgfile(input: File) {
    argfile = Some(input)
  }

  private var sourceDir: Option[File] = None
  private var failOnError: Boolean = true
  private var timeout: Option[Long] = None
  private var jvmArgs: Option[String] = None
  private var argfile: Option[File] = None

  private def createMapper() = {
    val mapper = new GlobPatternMapper()
    val extension = if (isMSIL) "*.msil" else "*.class"
    mapper setTo extension
    mapper setFrom "*.scala"

    mapper
  }

  override def execute() {
    def plural(x: Int) = if (x > 1) "s" else ""

    log("Executing ant task scalacfork, origin: %s".format(originOfThis), Project.MSG_VERBOSE)

    val compilerPath = this.compilerPath getOrElse error("Mandatory attribute 'compilerpath' is not set.")
    val sourceDir = this.sourceDir getOrElse error("Mandatory attribute 'srcdir' is not set.")
    val destinationDir = this.destinationDir getOrElse error("Mandatory attribute 'destdir' is not set.")

    val settings = new Settings
    settings.d = destinationDir

    compTarget foreach (settings.target = _)
    compilationPath foreach (settings.classpath = _)
    sourcePath foreach (settings.sourcepath = _)
    params foreach (settings.more = _)

    if (isMSIL)
      settings.sourcedir = sourceDir

    val mapper = createMapper()

    val includedFiles: Array[File] =
      new SourceFileScanner(this).restrict(
        getDirectoryScanner(sourceDir).getIncludedFiles,
        sourceDir,
        destinationDir,
        mapper
      ) map (x => new File(sourceDir, x))

    /** Nothing to do. */
    if (includedFiles.isEmpty && argfile.isEmpty)
      return

    if (includedFiles.nonEmpty)
      log("Compiling %d file%s to %s".format(includedFiles.size, plural(includedFiles.size), destinationDir))

    argfile foreach (x => log("Using argfile file: @" + x))

    val java = new Java(this)  // set this as owner
    java setFork true
    // using 'setLine' creates multiple arguments out of a space-separated string
    jvmArgs foreach (java.createJvmarg() setLine _)
    timeout foreach (java setTimeout _)

    java setClasspath compilerPath
    java setClassname MainClass

    // dump the arguments to a file and do "java @file"
    val tempArgFile = io.File.makeTemp("scalacfork")
    val tokens = settings.toArgs ++ (includedFiles map (_.getPath))
    tempArgFile writeAll (tokens mkString " ")

    val paths = List(Some(tempArgFile.toAbsolute.path), argfile).flatten map (_.toString)
    val res = execWithArgFiles(java, paths)

    if (failOnError && res != 0)
      error("Compilation failed because of an internal compiler error;"+
            " see the error output for details.")
  }
}