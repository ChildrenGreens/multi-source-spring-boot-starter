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

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.data.redis.autoconfigure.health.DataRedisHealthContributorAutoConfiguration;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class RedisMultiSourcesIT {

    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7.4.2-alpine");

    @Container
    private static final RedisContainer ALPHA_REDIS = new RedisContainer(REDIS_IMAGE);

    @Container
    private static final RedisContainer BETA_REDIS = new RedisContainer(REDIS_IMAGE);

    @Test
    void readsAndWritesThroughIndependentLettuceAndJedisSources() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        DataRedisAutoConfiguration.class,
                        RedisMultiSourcesAutoConfiguration.class,
                        DataRedisHealthContributorAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.multi-sources.redis.primary-key=alpha",
                        "spring.multi-sources.redis.sources.alpha.host=" + ALPHA_REDIS.getRedisHost(),
                        "spring.multi-sources.redis.sources.alpha.port=" + ALPHA_REDIS.getRedisPort(),
                        "spring.multi-sources.redis.sources.alpha.client-type=lettuce",
                        "spring.multi-sources.redis.sources.beta.host=" + BETA_REDIS.getRedisHost(),
                        "spring.multi-sources.redis.sources.beta.port=" + BETA_REDIS.getRedisPort(),
                        "spring.multi-sources.redis.sources.beta.client-type=jedis"
                )
                .run((context) -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean("alphaLettuceConnectionFactory"))
                            .isInstanceOf(LettuceConnectionFactory.class);
                    assertThat(context.getBean("betaJedisConnectionFactory"))
                            .isInstanceOf(JedisConnectionFactory.class);
                    assertThat(context.getBeanFactory()
                            .getBeanDefinition("alphaLettuceConnectionFactory").isPrimary()).isTrue();
                    assertThat(context.getBeanFactory()
                            .getBeanDefinition("betaJedisConnectionFactory").isPrimary()).isFalse();

                    StringRedisTemplate alpha = context.getBean(
                            "alphaStringRedisTemplate", StringRedisTemplate.class);
                    StringRedisTemplate beta = context.getBean(
                            "betaStringRedisTemplate", StringRedisTemplate.class);

                    alpha.opsForValue().set("shared-key", "alpha-value");
                    beta.opsForValue().set("shared-key", "beta-value");

                    assertThat(alpha.opsForValue().get("shared-key")).isEqualTo("alpha-value");
                    assertThat(beta.opsForValue().get("shared-key")).isEqualTo("beta-value");
                    assertThat(context.getBean(StringRedisTemplate.class)).isSameAs(alpha);

                    CompositeHealthContributor healthContributor = context.getBean(
                            "redisHealthContributor", CompositeHealthContributor.class);
                    assertHealthUp(healthContributor, "alphaLettuceConnectionFactory");
                    assertHealthUp(healthContributor, "betaJedisConnectionFactory");
                });
    }

    private void assertHealthUp(CompositeHealthContributor contributor, String name) {
        assertThat(contributor.getContributor(name)).isInstanceOf(HealthIndicator.class);
        HealthIndicator indicator = (HealthIndicator) contributor.getContributor(name);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }
}
