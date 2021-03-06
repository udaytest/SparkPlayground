package com.github.pedrovgs.sparkplayground.exercise12

import com.github.pedrovgs.sparkplayground.{Resources, SparkApp}
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

object Tweets extends SparkApp with Resources {

  private lazy val plainTweets: RDD[Array[String]] = readTweets("/exercise12/tweets.csv")

  private lazy val positiveTweets = plainTweets
    .filter { values =>
      values(0).toInt == 4
    }
    .cache()

  private lazy val plainExtraTweets: RDD[Array[String]] = readTweets("/exercise12/tweets2.csv")

  private lazy val tweetsBySentiments: RDD[(String, String)] = plainTweets
    .map { values =>
      val sentiment = values(0)
      val content   = values(5)
      (sentiment, content)
    }
    .cache()

  private lazy val extraTweetsBySentiments: RDD[(String, String)] = plainExtraTweets
    .map { values =>
      val sentiment = values(0)
      val content   = values(5)
      (sentiment, content)
    }
    .cache()

  private lazy val everyTweetBySentiments: RDD[(String, String)] = tweetsBySentiments
    .join(extraTweetsBySentiments)
    .mapValues {
      case (t1, t2) => t1 + t2
    }
    .cache()

  private lazy val everyTweetBySentimentsWithCogroup: RDD[(String, String)] = tweetsBySentiments
    .cogroup(extraTweetsBySentiments)
    .mapValues {
      case (t1, t2) => (t1 ++ t2).toString()
    }
    .cache()

  lazy val mostTweetedAuthor: String = plainTweets
    .map { values: Array[String] =>
      val author = values(4)
      val tweets = values(5)
      (author, tweets)
    }
    .groupByKey
    .mapValues { tweets =>
      tweets.size
    }
    .sortBy(_._2)
    .first()
    ._1

  lazy val positiveTweetsCount: Long = positiveTweets
    .count()

  lazy val positiveWordsCount: Long = countSentimentWords(tweetsBySentiments, "4")

  lazy val negativeWordsCount: Long = countSentimentWords(tweetsBySentiments, "0")

  lazy val positiveWordsCount2: Long = countSentimentWords(everyTweetBySentiments, "4")

  lazy val positiveWordsCountWithCogroup: Long =
    countSentimentWords(everyTweetBySentimentsWithCogroup, "4")

  lazy val negativeWordsCount2: Long = countSentimentWords(everyTweetBySentiments, "0")

  lazy val positiveWordsCountedByKey: Long = countSentimentWords2(tweetsBySentiments, "4")

  pprint.pprintln("The author with more tweets is:" + mostTweetedAuthor)
  pprint.pprintln("The number of positive tweets is: " + positiveTweetsCount)
  pprint.pprintln("The number of words associated to positive tweets is: " + positiveWordsCount)
  pprint.pprintln(
    "The number of words associated to positive tweets is: " + positiveWordsCountedByKey)
  pprint.pprintln("The number of words associated to negative tweets is: " + negativeWordsCount)
  pprint.pprintln(
    "The number of words associated to positive tweets plus extra tweets is: " + positiveWordsCount2)
  pprint.pprintln(
    "The number of words associated to negative tweets plus extra tweets is: " + negativeWordsCount2)
  pprint.pprintln("Let's execute a time command using pipe")
  tweetsBySentiments.pipe("time")
  pprint.pprintln("Let's play with the RDD partitions!")
  positiveTweets.coalesce(positiveTweets.partitions.length - 1)
  plainTweets.repartition(2)
  everyTweetBySentiments.repartitionAndSortWithinPartitions(
    Partitioner.defaultPartitioner(everyTweetBySentiments))

  // cache() method performs the same effect than persist.
  // persist() method invocation is going to save the RDD content
  // into memory without serializing it!
  pprint.pprintln("Let's cache, persist and unpersist some RDDs")
  extraTweetsBySentiments.persist()
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.DISK_ONLY)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.DISK_ONLY_2)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.MEMORY_AND_DISK)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.MEMORY_AND_DISK_2)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.MEMORY_AND_DISK_SER)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.MEMORY_AND_DISK_SER_2)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.MEMORY_ONLY_SER)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.MEMORY_ONLY_SER_2)
  extraTweetsBySentiments.unpersist(true)
  extraTweetsBySentiments.persist(StorageLevel.OFF_HEAP)
  extraTweetsBySentiments.unpersist()

  private def readTweets(path: String): RDD[Array[String]] =
    sparkContext
      .textFile(getFilePath(path))
      .map(_.replace("\"", ""))
      .map(_.split(","))
      .cache()

  private def countSentimentWords(tweets: RDD[(String, String)], sentiment: String) = {
    tweets
      .mapValues(_.length)
      .aggregateByKey(0)(_ + _, _ + _)
      .collectAsMap()(sentiment)
  }

  private def countSentimentWords2(tweets: RDD[(String, String)], sentiment: String) = {
    tweets
      .mapValues(_.length)
      .countByKey()(sentiment)
  }
}
