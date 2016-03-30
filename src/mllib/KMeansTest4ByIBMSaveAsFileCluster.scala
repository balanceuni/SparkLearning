/**
 * @author xubo
 * KMeansTest4ByIBmSaveAsFileCluster
 * 参考：
 * （1） http://www.ibm.com/developerworks/cn/opensource/os-cn-spark-practice4/
 * （2）http://archive.ics.uci.edu/ml/datasets/Wholesale+customers#
 * 修改：
 * （1）输出误差平方和
 * （2）将数据存储到HDFS
 *
 * 本地运行：
 * "hdfs://219.219.220.149:9000/xubo/spark/data/mllib/kmeans/WholesaleCustomersDataTrain1.txt" "hdfs://219.219.220.149:9000/xubo/spark/data/mllib/kmeans/WholesaleCustomersDataTest1.txt" 3 20 10
 */
/**
 * train and test file:txt
 * Channel Region Fresh Milk Grocery Frozen Detergents_Paper Delicassen
 * 2 3 12669 9656 7561 214 2674 1338
 * 2 3 7057 9810 9568 1762 3293 1776
 * 2 3 6353 8808 7684 2405 3516 7844
 */
package mllib

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import org.apache.spark.mllib.linalg.Vectors
import java.text.SimpleDateFormat
import java.util.Date
object KMeansTest4ByIBmSaveAsFileCluster {
  def main(args: Array[String]) {

    //判断输入
    if (args.length < 5) {
      println("Usage:KMeansClustering trainingDataFilePath testDataFilePath numClusters numIterations runTimes")
      sys.exit(1)
    }

    //configure
    val conf = new SparkConf().setAppName("Spark MLlib Exercise:K-Means Clustering(KMeansTest4ByIBmSaveAsFileCluster)")
    val sc = new SparkContext(conf)
    val iString = new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date())

    val rawTrainingData = sc.textFile(args(0))
    val parsedTrainingData = rawTrainingData.filter(!isColumnNameLine(_)).map(line => {
      Vectors.dense(line.split("\t").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
    }).cache()

    // Cluster the data into two classes using KMeans
    val numClusters = args(2).toInt
    val numIterations = args(3).toInt
    val runTimes = args(4).toInt
    var clusterIndex: Int = 0

    //查看输入参数是否正确
    //    for(i<-args) println(i);

    //KMeans.train
    val clusters: KMeansModel = KMeans.train(parsedTrainingData, numClusters, numIterations, runTimes)
    println("Cluster Number:" + clusters.clusterCenters.length)
    println("Cluster Centers Information Overview:")
    clusters.clusterCenters.foreach(
      x => {
        println("Center Point of Cluster " + clusterIndex + ":")
        println(x)
        clusterIndex += 1
      })

    val result1 = parsedTrainingData.map {
      line =>
        val prediction = clusters.predict(line)
        line + " " + prediction
    }.saveAsTextFile("hdfs://219.219.220.149:9000/xubo/spark/output/mllib/KMeansTest4ByIBmSaveAsFileCluster/" + iString + "/train")

    //output  Sum of Squared Errors
    println();
    val cost = clusters.computeCost(parsedTrainingData)
    println("Within Set Sum of Squared Errors = " + cost)
    println();

    //begin to check which cluster each test data belongs to based on the clustering result
    val rawTestData = sc.textFile(args(1))
    /*add .filter(!isColumnNameLine(_))*/
    val parsedTestData = rawTestData.filter(!isColumnNameLine(_)).map(line =>
      {
        Vectors.dense(line.split("\t").map(_.trim).filter(!"".equals(_)).map(_.toDouble))
      })
    parsedTestData.collect().foreach(testDataLine => {
      val predictedClusterIndex: Int = clusters.predict(testDataLine)
      println("The data " + testDataLine.toString + " belongs to cluster " +
        predictedClusterIndex)
    })
    println("Spark MLlib K-means clustering test finished. And SaveAsFile...")

    //test data SaveAsFile
    val result2 = parsedTestData.map {
      line =>
        val prediction = clusters.predict(line)
        line + " " + prediction
    }.saveAsTextFile("hdfs://219.219.220.149:9000/xubo/spark/output/mllib/KMeansTest4ByIBmSaveAsFileCluster/" + iString + "/test")
    println("SaveAsFile success");
  }

  private def isColumnNameLine(line: String): Boolean = {
    if (line != null && line.contains("Channel")) true
    else false
  }
}