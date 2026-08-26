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
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.amqp.autoconfigure.RabbitAutoConfiguration;
import org.springframework.boot.amqp.autoconfigure.health.RabbitHealthContributorAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RabbitMultiSourcesIT {

    private static final DockerImageName RABBITMQ_IMAGE =
            DockerImageName.parse("rabbitmq:4.1.0-management-alpine");

    private static final String QUEUE_NAME = "multi-source-integration-test";

    @Container
    private static final RabbitMQContainer ALPHA_RABBIT = new RabbitMQContainer(RABBITMQ_IMAGE);

    @Container
    private static final RabbitMQContainer BETA_RABBIT = new RabbitMQContainer(RABBITMQ_IMAGE);

    @Test
    void sendsAndReceivesMessagesThroughIndependentRabbitSources() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        RabbitAutoConfiguration.class,
                        RabbitMultiSourcesAutoConfiguration.class,
                        RabbitHealthContributorAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.multi-sources.rabbitmq.primary-key=alpha",
                        "spring.multi-sources.rabbitmq.sources.alpha.host=" + ALPHA_RABBIT.getHost(),
                        "spring.multi-sources.rabbitmq.sources.alpha.port=" + ALPHA_RABBIT.getAmqpPort(),
                        "spring.multi-sources.rabbitmq.sources.alpha.username=" + ALPHA_RABBIT.getAdminUsername(),
                        "spring.multi-sources.rabbitmq.sources.alpha.password=" + ALPHA_RABBIT.getAdminPassword(),
                        "spring.multi-sources.rabbitmq.sources.alpha.listener.type=simple",
                        "spring.multi-sources.rabbitmq.sources.beta.host=" + BETA_RABBIT.getHost(),
                        "spring.multi-sources.rabbitmq.sources.beta.port=" + BETA_RABBIT.getAmqpPort(),
                        "spring.multi-sources.rabbitmq.sources.beta.username=" + BETA_RABBIT.getAdminUsername(),
                        "spring.multi-sources.rabbitmq.sources.beta.password=" + BETA_RABBIT.getAdminPassword(),
                        "spring.multi-sources.rabbitmq.sources.beta.listener.type=direct",
                        "spring.rabbitmq.dynamic=false"
                )
                .run((context) -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("alphaCachingConnectionFactory"))
                            .isInstanceOf(CachingConnectionFactory.class);
                    assertThat(context.getBean("betaCachingConnectionFactory"))
                            .isInstanceOf(CachingConnectionFactory.class);
                    assertThat(context.getBean("alphaSimpleRabbitListenerContainerFactory"))
                            .isInstanceOf(SimpleRabbitListenerContainerFactory.class);
                    assertThat(context.getBean("betaDirectRabbitListenerContainerFactory"))
                            .isInstanceOf(DirectRabbitListenerContainerFactory.class);
                    assertThat(context.getBeanFactory()
                            .getBeanDefinition("alphaRabbitTemplate").isPrimary()).isTrue();
                    assertThat(context.getBeanFactory()
                            .getBeanDefinition("betaRabbitTemplate").isPrimary()).isFalse();

                    AmqpAdmin alphaAdmin = context.getBean("alphaAmqpAdmin", AmqpAdmin.class);
                    AmqpAdmin betaAdmin = context.getBean("betaAmqpAdmin", AmqpAdmin.class);
                    alphaAdmin.declareQueue(new Queue(QUEUE_NAME, false, false, true));
                    betaAdmin.declareQueue(new Queue(QUEUE_NAME, false, false, true));

                    RabbitTemplate alpha = context.getBean("alphaRabbitTemplate", RabbitTemplate.class);
                    RabbitTemplate beta = context.getBean("betaRabbitTemplate", RabbitTemplate.class);
                    alpha.convertAndSend(QUEUE_NAME, "alpha-message");
                    beta.convertAndSend(QUEUE_NAME, "beta-message");

                    assertThat(alpha.receiveAndConvert(QUEUE_NAME, 5_000)).isEqualTo("alpha-message");
                    assertThat(beta.receiveAndConvert(QUEUE_NAME, 5_000)).isEqualTo("beta-message");
                    assertThat(context.getBean(RabbitTemplate.class)).isSameAs(alpha);

                    CompositeHealthContributor healthContributor = context.getBean(
                            "rabbitHealthContributor", CompositeHealthContributor.class);
                    assertHealthUp(healthContributor, "alphaRabbitTemplate");
                    assertHealthUp(healthContributor, "betaRabbitTemplate");
                });
    }

    private void assertHealthUp(CompositeHealthContributor contributor, String name) {
        assertThat(contributor.getContributor(name)).isInstanceOf(HealthIndicator.class);
        HealthIndicator indicator = (HealthIndicator) contributor.getContributor(name);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
