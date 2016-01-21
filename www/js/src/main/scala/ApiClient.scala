package org.ukenergywatch.www

import boopickle.Default._
import java.nio.ByteBuffer
import scala.concurrent.Future
import org.scalajs.dom
import scala.scalajs.js.typedarray.{ ArrayBuffer, TypedArrayBuffer }
import scalajs.concurrent.JSExecutionContext.Implicits.runNow

object ApiClient extends autowire.Client[ByteBuffer, Pickler, Pickler] {
  override def doCall(req: Request): Future[ByteBuffer] = {
    dom.ext.Ajax.post(
      url = "/api/" + req.path.mkString("/"),
      data = Pickle.intoBytes(req.args),
      responseType = "arraybuffer",
      headers = Map("Content-Type" -> "application/octet-stream")
    ).map(r => TypedArrayBuffer.wrap(r.response.asInstanceOf[ArrayBuffer]))
  }
  override def read[R : Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
  override def write[R : Pickler](r: R) = Pickle.intoBytes(r)
}
