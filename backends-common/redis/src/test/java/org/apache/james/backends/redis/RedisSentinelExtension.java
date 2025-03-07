/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.backends.redis;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.inject.Singleton;

import org.apache.james.GuiceModuleTestExtension;
import org.apache.james.util.Runnables;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import com.github.fge.lambdas.Throwing;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Provides;

import io.lettuce.core.ReadFrom;

public class RedisSentinelExtension implements GuiceModuleTestExtension {
    private static final int SENTINEL_PORT = 26379;
    private static final String SENTINEL_PASSWORD = "321";
    private static final String START_REPLICA_COMMAND = "redis-server --appendonly yes --port 6379 --slaveof redis1 6379 --requirepass 123 --masterauth 123";
    private static final String START_MASTER_COMMAND = "redis-server --appendonly yes --port 6379 --requirepass 123 --masterauth 123";

    public static class RedisMasterReplicaContainerList extends ArrayList<DockerRedis> {
        public RedisMasterReplicaContainerList(Collection<? extends DockerRedis> c) {
            super(c);
        }

        public void pauseMasterNode() {
            GenericContainer container = this.get(0).getContainer();
            container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
        }

        public void unPauseMasterNode() {
            GenericContainer container = this.get(0).getContainer();
            if (container.getDockerClient().inspectContainerCmd(container.getContainerId())
                .exec()
                .getState()
                .getPaused()) {
                container.getDockerClient().unpauseContainerCmd(container.getContainerId()).exec();
            }
        }
    }

    public static class RedisSentinelContainerList extends ArrayList<DockerRedis> {
        public RedisSentinelContainerList(Collection<? extends DockerRedis> c) {
            super(c);
        }

        public SentinelRedisConfiguration getRedisConfiguration() {
            return SentinelRedisConfiguration.from(createRedisSentinelURI(),
                ReadFrom.MASTER,
                SENTINEL_PASSWORD);
        }

        public void pauseFirstNode() {
            GenericContainer container = this.get(0).getContainer();
            container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
        }

        public void unPauseFirstNode() {
            GenericContainer container = this.get(0).getContainer();
            if (container.getDockerClient().inspectContainerCmd(container.getContainerId())
                .exec()
                .getState()
                .getPaused()) {
                container.getDockerClient().unpauseContainerCmd(container.getContainerId()).exec();
            }
        }

        private String createRedisSentinelURI() {
            StringBuilder sb = new StringBuilder();
            sb.append("redis-sentinel://321@");

            sb.append(this.stream().map(container -> container.getContainer().getHost() + ":" + container.getContainer().getMappedPort(SENTINEL_PORT))
                .collect(Collectors.joining(",")));
            sb.append("?sentinelMasterId=mymaster");
            return sb.toString();
        }
    }

    public record RedisSentinelCluster(RedisMasterReplicaContainerList redisMasterReplicaContainerList,
                                       RedisSentinelContainerList redisSentinelContainerList) {
    }

    private final DockerRedis redis1;
    private final DockerRedis redis2;
    private final DockerRedis redis3;
    private final DockerRedis sentinel1;
    private final DockerRedis sentinel2;
    private final DockerRedis sentinel3;

    private RedisMasterReplicaContainerList redisMasterReplicaContainerList;
    private RedisSentinelContainerList redisSentinelContainerList;
    private RedisSentinelCluster redisSentinelCluster;
    private final Network network;

    public RedisSentinelExtension() {
        this.network = Network.newNetwork();
        redis1 = createRedisContainer("redis1", false);
        redis2 = createRedisContainer("redis2", true);
        redis3 = createRedisContainer("redis3", true);
        sentinel1 = createSentinelContainer("sentinel1");
        sentinel2 = createSentinelContainer("sentinel2");
        sentinel3 = createSentinelContainer("sentinel3");
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        redis1.start();
        redis2.start();
        redis3.start();
        sentinel1.start();
        sentinel2.start();
        sentinel3.start();
        redisMasterReplicaContainerList = new RedisMasterReplicaContainerList(List.of(redis1, redis2, redis3));
        redisSentinelContainerList = new RedisSentinelContainerList(List.of(sentinel1, sentinel2, sentinel3));
        redisSentinelCluster = new RedisSentinelCluster(redisMasterReplicaContainerList, redisSentinelContainerList);
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        Runnables.runParallel(
            sentinel1::stop,
            sentinel2::stop,
            sentinel3::stop,
            redis1::stop,
            redis2::stop,
            redis3::stop);
        network.close();
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        redisMasterReplicaContainerList.forEach(Throwing.consumer(container -> container.getContainer().execInContainer("redis-cli", "flushall")));
    }

    public RedisSentinelCluster getRedisSentinelCluster() {
        return redisSentinelCluster;
    }

    @Override
    public Module getModule() {
        return new AbstractModule() {
            @Provides
            @Singleton
            public RedisConfiguration provideRedisConfiguration() {
                return redisSentinelContainerList.getRedisConfiguration();
            }
        };
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType() == RedisSentinelCluster.class;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        return redisSentinelCluster;
    }

    private DockerRedis createRedisContainer(String alias, boolean isSlave) {
        DockerRedis redis = new DockerRedis(alias);

        if (isSlave) {
            redis.getContainer().withCommand(START_REPLICA_COMMAND);
        } else {
            redis.getContainer().withCommand(START_MASTER_COMMAND);
        }

        return redis;
    }

    private DockerRedis createSentinelContainer(String alias) {
        DockerRedis redisSentinel = new DockerRedis(alias);

        redisSentinel.getContainer()
            .withExposedPorts(SENTINEL_PORT)
            .withCommand("redis-sentinel /etc/redis/sentinel.conf")
            .withClasspathResourceMapping("sentinel.conf",
                "/etc/redis/sentinel.conf",
                BindMode.READ_ONLY)
            .waitingFor(Wait.forLogMessage(".*monitor master.*", 1)
                .withStartupTimeout(Duration.ofMinutes(2)));

        return redisSentinel;
    }
}