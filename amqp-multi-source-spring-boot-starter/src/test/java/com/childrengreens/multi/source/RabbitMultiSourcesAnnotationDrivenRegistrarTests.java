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
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RabbitMultiSourcesAnnotationDrivenRegistrar}.
 */
class RabbitMultiSourcesAnnotationDrivenRegistrarTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RabbitAutoConfiguration.class,
                    RabbitMultiSourcesAutoConfiguration.class
            ))
            .withPropertyValues(
                    "spring.multi-sources.rabbitmq.primary-key=alpha",
                    // alpha -> SIMPLE listener container
                    "spring.multi-sources.rabbitmq.sources.alpha.host=localhost",
                    "spring.multi-sources.rabbitmq.sources.alpha.port=5672",
                    "spring.multi-sources.rabbitmq.sources.alpha.listener.type=simple",
                    // beta -> DIRECT listener container
                    "spring.multi-sources.rabbitmq.sources.beta.host=localhost",
                    "spring.multi-sources.rabbitmq.sources.beta.port=5673",
                    "spring.multi-sources.rabbitmq.sources.beta.listener.type=direct"
            );

    @Test
    void registersSimpleAndDirectListenerContainerFactoriesPerSource() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasBean("alphaSimpleRabbitListenerContainerFactoryConfigurer");
            assertThat(context).hasBean("alphaSimpleRabbitListenerContainerFactory");
            assertThat(context).hasBean("betaDirectRabbitListenerContainerFactoryConfigurer");
            assertThat(context).hasBean("betaDirectRabbitListenerContainerFactory");

            assertThat(context.getBean("alphaSimpleRabbitListenerContainerFactory"))
                    .isInstanceOf(SimpleRabbitListenerContainerFactory.class);
            assertThat(context.getBean("betaDirectRabbitListenerContainerFactory"))
                    .isInstanceOf(DirectRabbitListenerContainerFactory.class);
        });
    }
}
