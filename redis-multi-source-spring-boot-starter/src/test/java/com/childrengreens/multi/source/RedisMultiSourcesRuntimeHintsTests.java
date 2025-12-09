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

import io.lettuce.core.resource.ClientResources;
import org.junit.jupiter.api.Test;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class RedisMultiSourcesRuntimeHintsTests {

    @Test
    void registersReflectionHintsForLettuce() {
        RuntimeHints hints = new RuntimeHints();
        new RedisMultiSourcesRuntimeHints().registerHints(hints, getClass().getClassLoader());

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TypeReference.of("org.springframework.boot.data.redis.autoconfigure.PropertiesDataRedisConnectionDetails")))
                .accepts(hints);

        assertThat(RuntimeHintsPredicates.reflection()
                .onConstructorInvocation(resolveConstructor("org.springframework.boot.data.redis.autoconfigure.LettuceConnectionConfiguration",
                        DataRedisProperties.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, DataRedisConnectionDetails.class)))
                .accepts(hints);

        assertThat(RuntimeHintsPredicates.reflection()
                .onMethodInvocation(resolveMethod("org.springframework.boot.data.redis.autoconfigure.LettuceConnectionConfiguration",
                        "createConnectionFactory", ObjectProvider.class, ObjectProvider.class, ClientResources.class)))
                .accepts(hints);
    }

    @Test
    void registersReflectionHintsForJedisWhenPresent() {
        RuntimeHints hints = new RuntimeHints();
        ClassLoader classLoader = getClass().getClassLoader();
        new RedisMultiSourcesRuntimeHints().registerHints(hints, classLoader);

        if (ClassUtils.isPresent("redis.clients.jedis.Jedis", classLoader)) {
            assertThat(RuntimeHintsPredicates.reflection()
                    .onConstructorInvocation(resolveConstructor("org.springframework.boot.data.redis.autoconfigure.JedisConnectionConfiguration",
                            DataRedisProperties.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, ObjectProvider.class, DataRedisConnectionDetails.class)))
                    .accepts(hints);

            assertThat(RuntimeHintsPredicates.reflection()
                    .onMethodInvocation(resolveMethod("org.springframework.boot.data.redis.autoconfigure.JedisConnectionConfiguration",
                            "createJedisConnectionFactory", ObjectProvider.class)))
                    .accepts(hints);
        }
    }

    @Test
    void failsWhenRequiredClassesMissing() {
        ClassLoader missingClasses = new ClassLoader(getClass().getClassLoader()) {
            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (name.equals("org.springframework.boot.data.redis.autoconfigure.PropertiesDataRedisConnectionDetails")) {
                    throw new ClassNotFoundException(name);
                }
                return super.loadClass(name, resolve);
            }
        };

        RuntimeHints hints = new RuntimeHints();
        assertThatThrownBy(() -> new RedisMultiSourcesRuntimeHints().registerHints(hints, missingClasses))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to resolve class for runtime hints");
    }

    private Class<?> resolveClass(String className) {
        try {
            return ClassUtils.forName(className, getClass().getClassLoader());
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Class not found in test runtime: " + className, ex);
        }
    }

    private Constructor<?> resolveConstructor(String className, Class<?>... parameterTypes) {
        try {
            return resolveClass(className).getDeclaredConstructor(parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Constructor not found for: " + className, ex);
        }
    }

    private Method resolveMethod(String className, String methodName, Class<?>... parameterTypes) {
        try {
            return resolveClass(className).getDeclaredMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Method not found for: " + className + "." + methodName, ex);
        }
    }
}
