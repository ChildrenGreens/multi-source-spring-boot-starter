/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.childrengreens.multi.source;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMultiSourcesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RabbitAutoConfiguration.class,
                    RabbitMultiSourcesAutoConfiguration.class
            ))
            .withPropertyValues(
                    // Multi-source configuration
                    "spring.multi-sources.rabbitmq.primary-key=alpha",
                    "spring.multi-sources.rabbitmq.sources.alpha.host=localhost",
                    "spring.multi-sources.rabbitmq.sources.alpha.port=5672",
                    "spring.multi-sources.rabbitmq.sources.beta.host=localhost",
                    "spring.multi-sources.rabbitmq.sources.beta.port=5673",
                    // Ensure AmqpAdmin is created by our registrar
                    "spring.rabbitmq.dynamic=false"
            );

    @Test
    void registersMultipleRabbitBeans() {
        this.contextRunner.run((context) -> {
            // Connection factories
            assertThat(context).hasBean("alphaCachingConnectionFactory");
            assertThat(context).hasBean("betaCachingConnectionFactory");

            // Templates
            assertThat(context).hasBean("alphaRabbitTemplate");
            assertThat(context).hasBean("betaRabbitTemplate");
            assertThat(context).hasBean("alphaRabbitMessagingTemplate");
            assertThat(context).hasBean("betaRabbitMessagingTemplate");

            // Admins (registered when spring.rabbitmq.dynamic=false)
            assertThat(context).hasBean("alphaAmqpAdmin");
            assertThat(context).hasBean("betaAmqpAdmin");

            assertThat(context.getBeansOfType(CachingConnectionFactory.class))
                    .containsKeys("alphaCachingConnectionFactory", "betaCachingConnectionFactory");
            assertThat(context.getBeansOfType(RabbitTemplate.class))
                    .containsKeys("alphaRabbitTemplate", "betaRabbitTemplate");
            assertThat(context.getBeansOfType(RabbitMessagingTemplate.class))
                    .containsKeys("alphaRabbitMessagingTemplate", "betaRabbitMessagingTemplate");
            assertThat(context.getBeansOfType(AmqpAdmin.class))
                    .containsKeys("alphaAmqpAdmin", "betaAmqpAdmin");
        });
    }
}

