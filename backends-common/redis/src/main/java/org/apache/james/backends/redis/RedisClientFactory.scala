/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 * http://www.apache.org/licenses/LICENSE-2.0                   *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.backends.redis

import java.time.Duration

import io.lettuce.core.cluster.{ClusterClientOptions, RedisClusterClient}
import io.lettuce.core.resource.ClientResources
import io.lettuce.core.{AbstractRedisClient, ClientOptions, RedisClient, SslOptions}
import jakarta.inject.{Inject, Singleton}
import org.apache.james.filesystem.api.FileSystem
import org.apache.james.util.concurrent.NamedThreadFactory

import scala.jdk.CollectionConverters._

class RedisClientFactory @Singleton() @Inject()
(fileSystem: FileSystem, redisConfiguration: RedisConfiguration) {
  def createRawRedisClient(): AbstractRedisClient = redisConfiguration match {
    case standaloneRedisConfiguration: StandaloneRedisConfiguration => createStandaloneClient(standaloneRedisConfiguration)
    case masterReplicaRedisConfiguration: MasterReplicaRedisConfiguration => createMasterReplicaClient(masterReplicaRedisConfiguration)
    case clusterRedisConfiguration: ClusterRedisConfiguration => createClusterClient(clusterRedisConfiguration)
    case sentinelRedisConfiguration: SentinelRedisConfiguration => createSentinelClient(sentinelRedisConfiguration)
  }

  def createStandaloneClient(standaloneRedisConfiguration: StandaloneRedisConfiguration): RedisClient =
    createStandaloneClient(standaloneRedisConfiguration, Option.empty)

  def createStandaloneClient(standaloneRedisConfiguration: StandaloneRedisConfiguration, timeout: Duration): RedisClient =
    createStandaloneClient(standaloneRedisConfiguration, Option.apply(timeout))

  def createStandaloneClient(standaloneRedisConfiguration: StandaloneRedisConfiguration, maybeTimeout: Option[Duration]): RedisClient = {
    maybeTimeout.foreach(timeout => standaloneRedisConfiguration.redisURI.setTimeout(timeout))
    val redisClient = RedisClient.create(standaloneRedisConfiguration.redisURI)
    redisClient.setOptions(createClientOptions(standaloneRedisConfiguration.useSSL, standaloneRedisConfiguration.mayBeSSLConfiguration))
    redisClient
  }

  def createClusterClient(clusterRedisConfiguration: ClusterRedisConfiguration): RedisClusterClient =
    createClusterClient(clusterRedisConfiguration, Option.empty)

  def createClusterClient(clusterRedisConfiguration: ClusterRedisConfiguration, timeout: Duration): RedisClusterClient =
    createClusterClient(clusterRedisConfiguration, Option.apply(timeout))

  def createClusterClient(clusterRedisConfiguration: ClusterRedisConfiguration, maybeTimeout: Option[Duration]): RedisClusterClient = {
    val resourceBuilder: ClientResources.Builder = ClientResources.builder()
      .threadFactoryProvider(poolName => NamedThreadFactory.withName(s"redis-driver-$poolName"))
    clusterRedisConfiguration.ioThreads.foreach(value => resourceBuilder.ioThreadPoolSize(value))
    clusterRedisConfiguration.workerThreads.foreach(value => resourceBuilder.computationThreadPoolSize(value))
    val redisClient = RedisClusterClient.create(resourceBuilder.build(),
      clusterRedisConfiguration.redisURI.value
        .map(rURI => {
          maybeTimeout.foreach(timeout => rURI.setTimeout(timeout))
          rURI
        }).asJava)
    redisClient.setOptions(ClusterClientOptions.builder(
        createClientOptions(clusterRedisConfiguration.useSSL, clusterRedisConfiguration.mayBeSSLConfiguration))
      .build())
    redisClient
  }

  def createMasterReplicaClient(masterReplicaRedisConfiguration: MasterReplicaRedisConfiguration): RedisClient = {
    val redisClient = RedisClient.create
    redisClient.setOptions(createClientOptions(masterReplicaRedisConfiguration.useSSL, masterReplicaRedisConfiguration.mayBeSSLConfiguration))
    redisClient
  }

  def createSentinelClient(sentinelRedisConfiguration: SentinelRedisConfiguration): RedisClient = {
    val redisClient = RedisClient.create
    redisClient.setOptions(createClientOptions(sentinelRedisConfiguration.useSSL, sentinelRedisConfiguration.mayBeSSLConfiguration))
    redisClient
  }

  private def createClientOptions(useSSL: Boolean, mayBeSSLConfiguration: Option[SSLConfiguration]): ClientOptions = {
    val clientOptionsBuilder = ClientOptions.builder
    if (useSSL) {
      mayBeSSLConfiguration.foreach(sslConfig => {
        if (!sslConfig.ignoreCertificateCheck) {
          sslConfig.maybeKeyStore.foreach(redisKeyStore => {
            val sslOptions = SslOptions.builder.jdkSslProvider.keystore(fileSystem.getFile(redisKeyStore.keyStoreFilePath), redisKeyStore.keyStorePassword.toCharArray).build
            clientOptionsBuilder.sslOptions(sslOptions)
          })
        }
      })
    }
    clientOptionsBuilder.build()
  }
}
