package org.cxdpublish.application

/**
 * Author Keshav Singh
 */

object SampleApp {
  def main(args: Array[String]): Unit = {
    val pd = new ProcessData
    val sparkSession = pd.getSpark
    pd.processDataFunction(sparkSession)
  }
  }