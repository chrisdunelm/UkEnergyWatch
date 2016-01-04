package org.ukenergywatch.utils

trait ElexonParamsComponent {
  def elexonParams: ElexonParams
  trait ElexonParams {
    def key: String
  }
}

trait ElexonFlagParamsComponent extends ElexonParamsComponent {
  this: FlagsComponent =>
  object elexonParams extends ElexonParams {
    object Flags extends FlagsBase {
      val elexonKey = flag[String](name = "elexonKey")
    }
    def key: String = Flags.elexonKey()
  }
  elexonParams.Flags // Early flags initialisation
}
