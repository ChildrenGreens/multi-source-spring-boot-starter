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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.data.redis.*;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Dynamically create multiple {@link RedisConnectionDetails} and {@link LettuceConnectionFactory} or {@link JedisConnectionFactory} based on Environment.
 *
 * @author ChildrenGreens
 */
public class RedisConnectionMultiSourcesRegistrar extends AbstractMultiSourcesRegistrar<RedisProperties> {


    @Override
    void registerBeanDefinitionsForSource(String name, RedisProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            // register PropertiesRedisConnectionDetails
            String redisConnectionDetailsBeanName = generateBeanName(RedisDataClassNames.PROPERTIES_DATA_REDIS_CONNECTION_DETAILS, name);
            registerBeanDefinition(registry,
                    RedisConnectionDetails.class,
                    redisConnectionDetailsBeanName,
                    isPrimary,
                    () -> {
                        ObjectProvider<SslBundles> sslBundlesProvider = beanFactory.getBeanProvider(SslBundles.class);
                        return (RedisConnectionDetails) newInstance(RedisDataClassNames.PROPERTIES_DATA_REDIS_CONNECTION_DETAILS, new Class[]{RedisProperties.class, SslBundles.class}, source, sslBundlesProvider.getIfAvailable());
                    });

            // JedisConnectionFactory or LettuceConnectionFactory
            boolean isJedisConnectionFactory = Objects.nonNull(source.getClientType())
                    && source.getClientType() == RedisProperties.ClientType.JEDIS
                    && ClassUtils.isPresent(RedisDataClassNames.JEDIS_TYPE, ClassUtils.getDefaultClassLoader());
            Class<? extends RedisConnectionFactory> redisConnectionFactory = isJedisConnectionFactory ? JedisConnectionFactory.class : LettuceConnectionFactory.class;

            // register RedisConnectionFactory
            registerBeanDefinition(registry,
                    RedisConnectionFactory.class,
                    generateBeanName(redisConnectionFactory, name),
                    isPrimary,
                    () -> {
                        // new JedisConnectionFactory or LettuceConnectionFactory
                        RedisConnectionDetails connectionDetails = beanFactory.getBean(redisConnectionDetailsBeanName, RedisConnectionDetails.class);
                        ObjectProvider<RedisStandaloneConfiguration> standaloneProvider = beanFactory.getBeanProvider(RedisStandaloneConfiguration.class);
                        ObjectProvider<RedisSentinelConfiguration> sentinelProvider = beanFactory.getBeanProvider(RedisSentinelConfiguration.class);
                        ObjectProvider<RedisClusterConfiguration> clusterProvider = beanFactory.getBeanProvider(RedisClusterConfiguration.class);

                        try {
                            String connectionConfigurationClassName = isJedisConnectionFactory ? RedisDataClassNames.JEDIS_CONNECTION_CONFIGURATION : RedisDataClassNames.LETTUCE_CONNECTION_CONFIGURATION;
                            Class<?> clazz = ClassUtils.forName(connectionConfigurationClassName, ClassUtils.getDefaultClassLoader());
                            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
                            constructor.setAccessible(true);
                            Object configuration = constructor.newInstance(source,
                                    standaloneProvider,
                                    sentinelProvider,
                                    clusterProvider,
                                    connectionDetails);


                            if (isJedisConnectionFactory) {
                                ObjectProvider<JedisClientConfigurationBuilderCustomizer> builderCustomizers = beanFactory.getBeanProvider(JedisClientConfigurationBuilderCustomizer.class);

                                Method createJedisConnectionFactory = clazz.getDeclaredMethod("createJedisConnectionFactory", ObjectProvider.class);
                                createJedisConnectionFactory.setAccessible(true);
                                JedisConnectionFactory factory = (JedisConnectionFactory) createJedisConnectionFactory.invoke(configuration, builderCustomizers);
                                if (isVirtualThreads()) {
                                    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("redis-");
                                    executor.setVirtualThreads(true);
                                    factory.setExecutor(executor);
                                }
                                return factory;
                            } else {
                                ObjectProvider<LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers = beanFactory.getBeanProvider(LettuceClientConfigurationBuilderCustomizer.class);
                                ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers = beanFactory.getBeanProvider(LettuceClientOptionsBuilderCustomizer.class);
                                ClientResources clientResources = beanFactory.getBean(ClientResources.class);

                                Method createConnectionFactory = clazz.getDeclaredMethod("createConnectionFactory", ObjectProvider.class, ObjectProvider.class, ClientResources.class);
                                createConnectionFactory.setAccessible(true);
                                LettuceConnectionFactory factory = (LettuceConnectionFactory) createConnectionFactory.invoke(configuration, clientConfigurationBuilderCustomizers, clientOptionsBuilderCustomizers, clientResources);
                                if (isVirtualThreads()) {
                                    SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("redis-");
                                    executor.setVirtualThreads(true);
                                    factory.setExecutor(executor);
                                }
                                return factory;
                            }
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 ClassNotFoundException | NoSuchMethodException e) {
                            throw new RuntimeException(e);
                        }
                    });

        }


    }

    @Override
    Class<? extends MultiSourcesProperties<RedisProperties>> getMultiSourcesPropertiesClass() {
        return RedisMultiSourcesProperties.class;
    }

}
