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
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RedisMultiSourcesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    RedisAutoConfiguration.class,
                    RedisMultiSourcesAutoConfiguration.class
            ))
            .withPropertyValues(
                    "spring.multi-sources.redis.primary-key=alpha",
                    "spring.multi-sources.redis.sources.alpha.host=localhost",
                    "spring.multi-sources.redis.sources.alpha.port=6379",
                    "spring.multi-sources.redis.sources.beta.host=localhost",
                    "spring.multi-sources.redis.sources.beta.port=6380"
            );

    @Test
    void registersMultipleRedisBeans() {
        this.contextRunner.run((context) -> {
            // Connection factories created by the registrar
            assertThat(context).hasBean("alphaLettuceConnectionFactory");
            assertThat(context).hasBean("betaLettuceConnectionFactory");

            // Templates created by the post-processor
            assertThat(context).hasBean("alphaRedisTemplate");
            assertThat(context).hasBean("betaRedisTemplate");
            assertThat(context).hasBean("alphaStringRedisTemplate");
            assertThat(context).hasBean("betaStringRedisTemplate");

            assertThat(context.getBeansOfType(RedisConnectionFactory.class))
                    .containsKeys("alphaLettuceConnectionFactory", "betaLettuceConnectionFactory");
            assertThat(context.getBeansOfType(RedisTemplate.class))
                    .containsKeys("alphaRedisTemplate", "betaRedisTemplate");
            assertThat(context.getBeansOfType(StringRedisTemplate.class))
                    .containsKeys("alphaStringRedisTemplate", "betaStringRedisTemplate");
        });
    }
}

