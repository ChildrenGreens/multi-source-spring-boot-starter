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
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;

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
            .withUserConfiguration(ContainerCustomizersConfiguration.class)
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

            SimpleRabbitListenerContainerFactory simpleFactory = context.getBean("alphaSimpleRabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);
            DirectRabbitListenerContainerFactory directFactory = context.getBean("betaDirectRabbitListenerContainerFactory", DirectRabbitListenerContainerFactory.class);
            assertThat(simpleFactory.createListenerContainer()).isNotNull();
            assertThat(directFactory.createListenerContainer()).isNotNull();
        });
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void usesVirtualThreadTaskExecutorWhenEnabled() {
        this.contextRunner
                .withPropertyValues("spring.threads.virtual.enabled=true")
                .run((context) -> {
                    TaskExecutor simpleExecutor = (TaskExecutor) resolveFieldWithHierarchy(
                            context.getBean("alphaSimpleRabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class)
                    );
                    TaskExecutor directExecutor = (TaskExecutor) resolveFieldWithHierarchy(
                            context.getBean("betaDirectRabbitListenerContainerFactory", DirectRabbitListenerContainerFactory.class)
                    );

                    assertThat(simpleExecutor).isInstanceOf(VirtualThreadTaskExecutor.class);
                    assertThat(directExecutor).isInstanceOf(VirtualThreadTaskExecutor.class);
                });
    }

    private Object resolveFieldWithHierarchy(Object target) {
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                var field = current.getDeclaredField("taskExecutor");
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ex) {
                current = current.getSuperclass();
            } catch (Exception ex) {
                throw new IllegalStateException("Failed to resolve field '" + "taskExecutor" + "' from " + target, ex);
            }
        }
        throw new IllegalStateException("Field '" + "taskExecutor" + "' not found in hierarchy of " + target);
    }

    @Configuration(proxyBeanMethods = false)
    static class ContainerCustomizersConfiguration {

        @Bean
        ContainerCustomizer<SimpleMessageListenerContainer> simpleContainerCustomizer() {
            return (container) -> container.setConcurrentConsumers(2);
        }

        @Bean
        ContainerCustomizer<DirectMessageListenerContainer> directContainerCustomizer() {
            return (container) -> container.setConsumersPerQueue(1);
        }
    }
}
