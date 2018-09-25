package neu.pdpmr.project

import org.apache.spark.ml.PipelineModel
import org.apache.spark.sql._
import scala.io.StdIn
import scala.collection.mutable.ArrayBuffer


/**
  * Created by futailin,ritvika on 12/06/17.
  */
object Model {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession
      .builder
      .appName("Model")
      .master("local[*]")
      .getOrCreate()

    val model = PipelineModel.load("LinearModel")

    val inputLines = spark.sparkContext.textFile("bigfile.csv")
      .mapPartitionsWithIndex { case (0, header) => header.drop(1) case (_, item) => item }
      .map(_.split(",")).map(x => (x(0), x(1), x(3).toDouble, x(4).toDouble, x(5).toDouble,
      x(6).toDouble, x(7).toDouble, x(8).toDouble, x(9).toDouble, x(10), x(11).toDouble, x(12), x(13).toDouble, x(14),
      x(15).toDouble, x(16).toDouble, x(17).toDouble, x(18).toDouble, x(19).toDouble, x(20).toDouble, x(21).toDouble))

    val columnName = Seq("artist_name", "title", "downloads", "mean_price", "fade_in", "fade_out", "duration",
      "loudness", "tempo", "key", "key_confidence", "mode", "mode_confidence", "time_signature", "time_signature_confidence", "artist_familiarity",
      "artist_hotness", "song_hotness", "year", "number_jam", "number_play")


    import spark.implicits._
    val df = inputLines.toDF(columnName: _*)

    val inputArtistName = ArrayBuffer[String]()
    val inputTitle = ArrayBuffer[String]()

    val bufferedSource = StdIn.readLine()

    while  (bufferedSource != "") {
      val lines = bufferedSource.split(";")
      inputArtistName += lines(0).toLowerCase().trim.replaceAll("[^A-Za-z]+", " ")
      inputTitle += lines(1).toLowerCase().trim.replaceAll("[^A-Za-z]+", " ")
    }

    val columnName2 = Seq("artist_name", "index")
    val inputArtDf = inputArtistName.zipWithIndex.toDF(columnName2: _*)
    //inputArtDf.show()

    val columnName3 = Seq("title", "index")
    val inputTitleDf = inputTitle.zipWithIndex.toDF(columnName3: _*)
    //inputTitleDf.show()

    val inputDf = inputArtDf.join(inputTitleDf, "index")

    val newRdd = spark.sparkContext.parallelize(inputArtistName)

    val filterArtistName = spark.udf.register("filterArtistName", (artistName: String) => {
      inputArtistName.contains(artistName)
    })
    val filterTitleName = spark.udf.register("filterTitleName", (title: String) => {
      inputTitle.contains(title)
    })

    val filtered = df.filter(filterArtistName(df("artist_name")) && filterTitleName(df("title")))
    val filtered2 = filtered.dropDuplicates(colNames = Seq("artist_name", "title"))

    //inputDf.join(filtered, Seq("artist_name", "title"), "left_outer").select("artist_name", "title", "downloads").show(110)

    var result = model.transform(filtered2)

    result = result.select("prediction","artist_name", "title")

    result = inputDf.join(result, Seq("artist_name", "title"), "left_outer").na.fill(0, Seq("prediction")).select("artist_name", "title", "prediction", "index")

    val intermResult = result.select("index", "prediction")
      .sort("index")
      .select("prediction")

    val artistName = result.filter(result("prediction") === 0).select("artist_name")

    val df2 = df.join(artistName, "artist_name").select("artist_name", "downloads").groupBy("artist_name").mean("downloads")

    result = result.join(df2, Seq("artist_name"), "left_outer").na.fill(0)
    //result.show(100)

    val replaceNoPrediction = spark.udf.register("replaceNoPrediction", (predicted: Double, avgDown: Double) => {
      predicted + avgDown
    })

    result = result.withColumn("predictionUpdated", replaceNoPrediction(result("prediction"), result("avg(downloads)")))
    result.sort("index").select("predictionUpdated").collect.foreach(x => println(x.get(0)))

  }

}

