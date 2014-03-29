package org.ukenergywatch.importer

trait ConfigComp {
  def config: Config
  trait Config {
    def getString(key: String): Option[String]
  }
}

trait FileConfig extends ConfigComp {
  def config = FileConfig
  object FileConfig extends Config {
    def getString(key: String): Option[String] = ???
  }
}
