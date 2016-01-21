package org.ukenergywatch.www

import boopickle.DefaultBasic._
import java.nio.ByteBuffer

object ApiRouter extends autowire.Server[ByteBuffer, Pickler, Pickler] {
  override def read[R : Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
  override def write[R : Pickler](r: R) = Pickle.intoBytes(r)
}

object ApiImpl extends Api {

  def getElectricSummary(): ElectricSummaryModel.Data = {
    // TODO
    ElectricSummaryModel.Data(4422, "success")
  }

}
