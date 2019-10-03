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

package org.apache.james.queue.rabbitmq.view;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.commons.configuration2.PropertiesConfiguration;
import org.junit.jupiter.api.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

class RabbitMQMailQueueSizeConfigurationTest {
    @Test
    void shouldMatchBeanContract() {
        EqualsVerifier.forClass(RabbitMQMailQueueSizeConfiguration.class).verify();
    }

    @Test
    void fromShouldReturnDefaultForEmptyConfiguration() {
        RabbitMQMailQueueSizeConfiguration actual = RabbitMQMailQueueSizeConfiguration.from(new PropertiesConfiguration());

        assertThat(actual)
            .isEqualTo(RabbitMQMailQueueSizeConfiguration.DEFAULT);
    }

    @Test
    void fromShouldReturnConfiguredSizeMetricsEnabled() {
        PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.addProperty(RabbitMQMailQueueSizeConfiguration.SIZE_METRICS_ENABLED_PROPERTY, false);
        RabbitMQMailQueueSizeConfiguration actual = RabbitMQMailQueueSizeConfiguration.from(configuration);

        assertThat(actual.isSizeMetricsEnabled())
            .isEqualTo(false);
    }
}
