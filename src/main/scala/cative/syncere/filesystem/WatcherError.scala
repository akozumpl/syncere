package cative.syncere.filesystem

case class WatcherError(msg: String) extends Exception(msg)
