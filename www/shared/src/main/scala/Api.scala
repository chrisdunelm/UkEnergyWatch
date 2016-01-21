package org.ukenergywatch.www

trait Api {
  def getElectricSummary(): ElectricSummaryModel.Data
}
