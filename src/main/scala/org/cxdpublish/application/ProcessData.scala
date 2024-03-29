package org.cxdpublish.application

/**
 * Author Keshav Singh
 */
import org.apache.hadoop.conf.Configuration
import org.apache.logging.log4j.LogManager
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.Row
import org.apache.spark.internal.Logging

import java.sql.Date


class ProcessData extends Serializable {
  def getSpark: SparkSession = {
    val log = LogManager.getRootLogger
    val spark = try {
      SparkSession.builder().getOrCreate()
    } catch {
      case _: org.apache.spark.SparkException => log.info("Using test configuration for spark")
        SparkSession.builder().master("local[*]").appName("App-test-launch").getOrCreate()
    }
    spark
  }
  def processDataFunction(sparkSession: SparkSession):Unit = {
  val spark = getSpark
  //val spark = SparkSession.builder().master("local").getOrCreate()
  val hadoopConfig: Configuration = spark.sparkContext.hadoopConfiguration
  hadoopConfig.set("fs.hdfs.impl", classOf[org.apache.hadoop.hdfs.DistributedFileSystem].getName)
  hadoopConfig.set("fs.file.impl", classOf[org.apache.hadoop.fs.LocalFileSystem].getName)

  val schema = StructType(Array(
    StructField("AirportCode", StringType, false),
    StructField("Date", DateType, false),
    StructField("TempHighF", IntegerType, false),
    StructField("TempLowF", IntegerType, false)
  ))

  val data = List(
    Row("BLI", Date.valueOf("2021-04-03"), 52, 43),
    Row("BLI", Date.valueOf("2021-04-02"), 50, 38),
    Row("BLI", Date.valueOf("2021-04-01"), 52, 41),
    Row("PDX", Date.valueOf("2021-04-03"), 64, 45),
    Row("PDX", Date.valueOf("2021-04-02"), 61, 41),
    Row("PDX", Date.valueOf("2021-04-01"), 66, 39),
    Row("SEA", Date.valueOf("2021-04-03"), 57, 43),
    Row("SEA", Date.valueOf("2021-04-02"), 54, 39),
    Row("SEA", Date.valueOf("2021-04-01"), 56, 41)
  )

  val rdd = spark.sparkContext.makeRDD(data)
  val temps = spark.createDataFrame(rdd, schema)
  //.createTempView("vwSQL")

  // Create a table on the Databricks cluster and then fill
  // the table with the DataFrame's contents.
  // If the table already exists from a previous run,
  // delete it first.
  spark.sql("USE default")
  spark.sql("DROP TABLE IF EXISTS demo_temps_table")
  temps.write.saveAsTable("demo_temps_table")

  // Query the table on the Databricks cluster, returning rows
  // where the airport code is not BLI and the date is later
  // than 2021-04-01. Group the results and order by high
  // temperature in descending order.
  val df_temps = spark.sql("SELECT * FROM demo_temps_table " +
    "WHERE AirportCode != 'BLI' AND Date > '2021-04-01' " +
    "GROUP BY AirportCode, Date, TempHighF, TempLowF " +
    "ORDER BY TempHighF DESC")
  df_temps.show()

  // Results:
  //
  // +-----------+----------+---------+--------+
  // |AirportCode|      Date|TempHighF|TempLowF|
  // +-----------+----------+---------+--------+
  // |        PDX|2021-04-03|       64|      45|
  // |        PDX|2021-04-02|       61|      41|
  // |        SEA|2021-04-03|       57|      43|
  // |        SEA|2021-04-02|       54|      39|
  // +-----------+----------+---------+--------+

  // Clean up by deleting the table from the Databricks cluster.
  spark.sql("DROP TABLE demo_temps_table")

}
}
