package cative.syncere

import com.monovore.decline.Help

case class CliError(help: Help) extends Exception(help.toString)
