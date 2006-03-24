/* NSC -- new Scala compiler
 * Copyright 2005-2006 LAMP/EPFL
 * @author  Martin Odersky
 */
// $Id: $

package scala.tools.nsc

/** a command line for the interpreter */
class InterpreterCommand(arguments: List[String], error: String => unit)
extends CompilerCommand(arguments, error, false) {
  override val cmdName = "scalaint"
  override val fileEnding = ".scalaint"
}