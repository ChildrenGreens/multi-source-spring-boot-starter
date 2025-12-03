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
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.data.redis.autoconfigure.DataRedisConnectionDetails;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Runtime hints for reflective Redis infrastructure access.
 *
 * @author ChildrenGreens
 */
public class RedisMultiSourcesRuntimeHints implements RuntimeHintsRegistrar {

    private static final String JEDIS_TYPE = "redis.clients.jedis.Jedis";
    private static final String PROPERTIES_REDIS_CONNECTION_DETAILS = "org.springframework.boot.data.redis.autoconfigure.PropertiesDataRedisConnectionDetails";
    private static final String LETTUCE_CONNECTION_CONFIGURATION = "org.springframework.boot.data.redis.autoconfigure.LettuceConnectionConfiguration";
    private static final String JEDIS_CONNECTION_CONFIGURATION = "org.springframework.boot.data.redis.autoconfigure.JedisConnectionConfiguration";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerConnectionDetails(hints, classLoader);
        registerLettuceConfiguration(hints, classLoader);
        if (ClassUtils.isPresent(JEDIS_TYPE, classLoader) && ClassUtils.isPresent(JEDIS_CONNECTION_CONFIGURATION, classLoader)) {
            registerJedisConfiguration(hints, classLoader);
        }
    }

    private void registerConnectionDetails(RuntimeHints hints, ClassLoader classLoader) {
        try {
            Class<?> connectionDetails = resolveClass(PROPERTIES_REDIS_CONNECTION_DETAILS, classLoader);
            Constructor<?> constructor = connectionDetails.getDeclaredConstructor(DataRedisProperties.class, SslBundles.class);
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to register Redis connection details constructor hint", ex);
        }
    }

    private void registerLettuceConfiguration(RuntimeHints hints, ClassLoader classLoader) {
        try {
            Class<?> configuration = resolveClass(LETTUCE_CONNECTION_CONFIGURATION, classLoader);
            Constructor<?> constructor = configuration
                    .getDeclaredConstructor(DataRedisProperties.class, ObjectProvider.class, ObjectProvider.class,
                            ObjectProvider.class, ObjectProvider.class, DataRedisConnectionDetails.class);
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
            Method createConnectionFactory = configuration.getDeclaredMethod("createConnectionFactory",
                    ObjectProvider.class, ObjectProvider.class, ClientResources.class);
            hints.reflection().registerMethod(createConnectionFactory, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to register Redis lettuce constructor hint", ex);
        }
    }

    private void registerJedisConfiguration(RuntimeHints hints, ClassLoader classLoader) {
        try {
            Class<?> configuration = resolveClass(JEDIS_CONNECTION_CONFIGURATION, classLoader);
            Constructor<?> constructor = configuration
                    .getDeclaredConstructor(DataRedisProperties.class, ObjectProvider.class, ObjectProvider.class,
                            ObjectProvider.class, ObjectProvider.class, DataRedisConnectionDetails.class);
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
            Method createConnectionFactory = configuration.getDeclaredMethod("createJedisConnectionFactory",
                    ObjectProvider.class);
            hints.reflection().registerMethod(createConnectionFactory, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to register Redis jedis constructor hint", ex);
        }
    }

    private Class<?> resolveClass(String className, ClassLoader classLoader) {
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to resolve class for runtime hints: " + className, ex);
        }
    }
}
