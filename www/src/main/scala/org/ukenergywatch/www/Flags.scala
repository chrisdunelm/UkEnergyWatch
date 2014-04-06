package org.ukenergywatch.www

import org.ukenergywatch.utils.Options

object Flags extends Options {
  register(org.ukenergywatch.slogger.impl.Slogger.Flags)

  val port = opt(9001)

}
