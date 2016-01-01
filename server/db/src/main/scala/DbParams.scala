package org.ukenergywatch.db

trait DbParamsComponent {
  def dbParams: DbParams
  trait DbParams {
    def host: String
    def database: String
    def user: String
    def password: String
  }
}
