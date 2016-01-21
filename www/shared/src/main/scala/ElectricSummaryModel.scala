package org.ukenergywatch.www

object ElectricSummaryModel {
  case class Data(
    aNumber: Int,
    aWord: String
  )
  object Data {
    import boopickle.DefaultBasic._
    implicit val pickler: Pickler[Data] = PicklerGenerator.generatePickler[Data]
  }

  val aNumber = Id("aNumber")
  val aWord = Id("aWord")
}

trait ElectricSummaryModel extends UpdateableModel[ElectricSummaryModel.Data] {
  import ElectricSummaryModel._
  import bundle.all._

  protected def fragmentDescs = Seq[(Id, Data => Tag)](
    aNumber -> (data => span(data.aNumber.toString)),
    aWord -> (data => span(data.aWord))
  )
}
