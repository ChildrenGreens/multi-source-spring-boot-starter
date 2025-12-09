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

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RedisConnectionMultiSourcesRegistrar}.
 */
class RedisConnectionMultiSourcesRegistrarTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    DataRedisAutoConfiguration.class,
                    RedisMultiSourcesAutoConfiguration.class
            ));

    @Test
    void registersLettuceConnectionFactoriesByDefault() {
        this.contextRunner
                .withPropertyValues(
                        "spring.multi-sources.redis.primary-key=alpha",
                        "spring.multi-sources.redis.sources.alpha.host=localhost",
                        "spring.multi-sources.redis.sources.alpha.port=6379",
                        "spring.multi-sources.redis.sources.beta.host=localhost",
                        "spring.multi-sources.redis.sources.beta.port=6380"
                )
                .run((context) -> {
                    assertThat(context).hasBean("alphaLettuceConnectionFactory");
                    assertThat(context).hasBean("betaLettuceConnectionFactory");
                    assertThat(context.getBeansOfType(RedisConnectionFactory.class))
                            .containsKeys("alphaLettuceConnectionFactory", "betaLettuceConnectionFactory");
                });
    }

    @Test
    void registersJedisConnectionFactoryWhenRequested() {
        Assumptions.assumeTrue(ClassUtils.isPresent("redis.clients.jedis.Jedis", getClass().getClassLoader()),
                "Jedis not on classpath");

        this.contextRunner
                .withPropertyValues(
                        "spring.multi-sources.redis.primary-key=alpha",
                        "spring.multi-sources.redis.sources.alpha.host=localhost",
                        "spring.multi-sources.redis.sources.alpha.port=6379",
                        "spring.multi-sources.redis.sources.alpha.client-type=jedis",
                        "spring.multi-sources.redis.sources.beta.host=localhost",
                        "spring.multi-sources.redis.sources.beta.port=6380"
                )
                .run((context) -> {
                    assertThat(context).hasBean("alphaJedisConnectionFactory");
                    assertThat(context).hasBean("betaLettuceConnectionFactory");

                    assertThat(context.getBean("alphaJedisConnectionFactory"))
                            .isInstanceOf(JedisConnectionFactory.class);
                    assertThat(context.getBean("betaLettuceConnectionFactory"))
                            .isInstanceOf(LettuceConnectionFactory.class);
                });
    }
}
