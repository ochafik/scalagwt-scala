/*                     __                                               *\
**     ________ ___   / /  ___     Scala API                            **
**    / __/ __// _ | / /  / _ |    (c) 2002-2008, LAMP/EPFL             **
**  __\ \/ /__/ __ |/ /__/ __ |    http://scala-lang.org/               **
** /____/\___/_/ |_/____/_/ | |                                         **
**                          |/                                          **
\*                                                                      */

// $Id$
package scala.tools.nsc
package backend
package jribble
import symtab.SymbolTable
import backend.icode.TypeKinds

/**
 * Utilities for formatting Scala constructs in Jribble syntax.
 * 
 *  @author  Lex Spoon
 */
trait JribbleFormatting {
  val global: SymbolTable { def abort(msg: String): Nothing }
  import global._
  protected val typeKinds: TypeKinds {
    val global: JribbleFormatting.this.global.type
  }
  import typeKinds._
  protected val scalaPrimitives: ScalaPrimitives {
    val global: JribbleFormatting.this.global.type
  }

  protected def jribbleName(sym: Symbol, fullyQualify: Boolean): String = {
    import symtab.Flags._
    def suffix = if (sym.isModuleClass && !sym.isTrait && !sym.hasFlag(JAVA)) "$" else ""
    //copy of AbsSymbol.fullName adapted to jribble syntax for fully qualified names which is
    //(package com.foo).Bar
    def fullName = {
      val separator = '.'
      def inParens(x: String) = "(" + x + ")"
      if (sym.isRoot || sym.isRootPackage || sym == scala.reflect.NoSymbol) sym.toString
      else if (sym.owner.isEffectiveRoot) sym.encodedName
      else inParens("package " + sym.owner.enclClass.fullName(separator)) + separator + sym.encodedName
    }

    // TODO(spoon): why the special cases?  double check that they are needed
    if (sym == definitions.NothingClass)
      return "scala.runtime.Nothing$"
    else if (sym == definitions.NullClass)
      return "scala.runtime.Null$"

    val name = if (fullyQualify) fullName else sym.simpleName
    name + suffix
  }
  
  protected def jribbleShortName(sym: Symbol): String =
    jribbleName(sym, false)
  
  protected def jribbleName(sym: Symbol): String =
    jribbleName(sym, true)
  
  protected def jribbleName(tpe: Type): String = {
    def tpstr(typ: TypeKind): String =
      typ match {
        case UNIT => "void" // TODO(spoon): depends on context?  a Scala variable can be of type unit!
        case BOOL            => "boolean"
        case BYTE            => "byte"
        case SHORT           => "short"
        case CHAR            => "char"
        case INT             => "int"
        case LONG            => "long"
        case FLOAT           => "float"
        case DOUBLE          => "double"
        case REFERENCE(cls)  => jribbleName(cls)
        case ARRAY(elem)     => tpstr(elem) + "[]"
      }
    return tpstr(toTypeKind(tpe))
  }

  protected def jribbleMethodSignature(tpe: Type): String = {
    val paramsTypeSymbols = tpe.paramTypes.map(_.typeSymbol)
    (tpe.resultType.typeSymbol :: paramsTypeSymbols).map(jribbleName).mkString("<", ", ", ">")
  }
  
  protected def jribblePrimName(prim: Int): String = {
    import scalaPrimitives._

    (prim : @unchecked) match {
      // Arithmetic unary operations
	  case POS => "+"                            // +x
	  case NEG => "-"                           // -x
	  case NOT => "~"                           // ~x
	
	  // Arithmetic binary operations
	  case ADD => "+"                          // x + y
	  case SUB => "-"                           // x - y
	  case MUL => "*"                           // x * y
	  case DIV => "/"                           // x / y
	  case MOD => "%"                           // x % y
	
	  // Bitwise operations
	  case OR  => "|"                           // x | y
	  case XOR => "^"                           // x ^ y
	  case AND => "&"                           // x & y
	
	  // Shift operations
	  case LSL => "<<"                           // x << y
	  case LSR => ">>"                           // x >>> y
	  case ASR => ">>>"                           // x >> y
	
	  // Comparison operations
	  case ID => "=="                            // x eq y
	  case NI => "!="                            // x ne y
	  case EQ => "=="                            // x == y
	  case NE => "!="                            // x != y
	  case LT => "<"                            // x < y
	  case LE => "<="                            // x <= y
	  case GE => ">="                            // x > y
	  case GT => ">"                            // x >= y
	
	  // Boolean unary operations
	  case ZNOT => "!"                          // !x
	
	  // Boolean binary operations
	  case ZOR => "||"                           // x || y
	  case ZAND => "&&"                          // x && y
	
	  // Array operations
	  case LENGTH => ""                        // x.length
	  case APPLY  => ""                        // x(y)
	  case UPDATE => ""                        // x(y) => ""z
	
	  // String operations
	  case CONCAT => "+"                       // String.valueOf(x)+String.valueOf(y)
    }
  }
}