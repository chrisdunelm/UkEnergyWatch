package org.ukenergywatch.utils

trait ElexonParamsComponent {

  def elexonParams: ElexonParams

  trait ElexonParams {
    def key: String
  }

}
