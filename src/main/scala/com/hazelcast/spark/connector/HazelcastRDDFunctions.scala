package com.hazelcast.spark.connector

import com.hazelcast.client.cache.impl.ClientCacheProxy
import com.hazelcast.core.HazelcastInstance
import org.apache.spark.TaskContext
import org.apache.spark.rdd.RDD


class HazelcastRDDFunctions[K, V](val rdd: RDD[(K, V)]) extends Serializable {

  def saveToHazelcastCache(cacheName: String): Unit = {
    val server: String = rdd.sparkContext.getConf.get("hazelcast.server.address")
    val job = (ctx: TaskContext, iterator: Iterator[(K, V)]) => {
      new HazelcastWriteToCacheJob().runJob(ctx, iterator, cacheName, server)
    }
    rdd.sparkContext.runJob(rdd, job)
  }

  private class HazelcastWriteToCacheJob() extends Serializable {
    def runJob(ctx: TaskContext, iterator: Iterator[(K, V)], cacheName: String, server: String): Unit = {
      val client: HazelcastInstance = ConnectionManager.getHazelcastConnection(server);
      val cache: ClientCacheProxy[K, V] = HazelcastHelper.getCacheFromClientProvider(cacheName, client)
      iterator.foreach((kv) => cache.put(kv._1, kv._2))
    }
  }

}