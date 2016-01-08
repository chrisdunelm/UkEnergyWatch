package org.ukenergywatch.www

import io.prometheus.client.exporter.MetricsServlet
import io.prometheus.client.Gauge

object Monitoring {
  val servletClass = classOf[MetricsServlet]

  val up = Gauge.build.name("up").help("Is this task up?").register()
}
