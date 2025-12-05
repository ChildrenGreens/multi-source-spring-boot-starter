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
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.data.redis.autoconfigure.*;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

/**
 * Dynamically create multiple {@link DataRedisConnectionDetails} and {@link LettuceConnectionFactory} or {@link JedisConnectionFactory} based on Environment.
 *
 * @author ChildrenGreens
 */
public class RedisConnectionMultiSourcesRegistrar extends AbstractMultiSourcesRegistrar<DataRedisProperties> {


    private static final String redisConnectionDetailsClassName = "org.springframework.boot.data.redis.autoconfigure.PropertiesDataRedisConnectionDetails";

    private static final String lettuceConnectionConfigurationClassName = "org.springframework.boot.data.redis.autoconfigure.LettuceConnectionConfiguration";

    private static final String jedisConnectionConfigurationClassName = "org.springframework.boot.data.redis.autoconfigure.JedisConnectionConfiguration";

    private static final boolean JEDIS_AVAILABLE = ClassUtils.isPresent("redis.clients.jedis.Jedis", ClassUtils.getDefaultClassLoader());


    @Override
    void registerBeanDefinitionsForSource(String name, DataRedisProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            // register PropertiesDataRedisConnectionDetails
            String redisConnectionDetailsBeanName = generateBeanName(redisConnectionDetailsClassName, name);
            registerBeanDefinition(registry,
                    DataRedisConnectionDetails.class,
                    redisConnectionDetailsBeanName,
                    isPrimary,
                    () -> {
                        ObjectProvider<@NonNull SslBundles> sslBundlesProvider = beanFactory.getBeanProvider(SslBundles.class);
                        return (DataRedisConnectionDetails) newInstance(redisConnectionDetailsClassName, new Class[]{DataRedisProperties.class, SslBundles.class}, source, sslBundlesProvider.getIfAvailable());
                    });

            // JedisConnectionFactory or LettuceConnectionFactory
            boolean isJedisConnectionFactory = Objects.nonNull(source.getClientType())
                    && source.getClientType() == DataRedisProperties.ClientType.JEDIS
                    && JEDIS_AVAILABLE;
            Class<? extends RedisConnectionFactory> redisConnectionFactory = isJedisConnectionFactory ? JedisConnectionFactory.class : LettuceConnectionFactory.class;

            // register RedisConnectionFactory
            registerBeanDefinition(registry,
                    RedisConnectionFactory.class,
                    generateBeanName(redisConnectionFactory, name),
                    isPrimary,
                    () -> {
                        // new JedisConnectionFactory or LettuceConnectionFactory
                        DataRedisConnectionDetails connectionDetails = beanFactory.getBean(redisConnectionDetailsBeanName, DataRedisConnectionDetails.class);
                        ObjectProvider<@NonNull RedisStandaloneConfiguration> standaloneProvider = beanFactory.getBeanProvider(RedisStandaloneConfiguration.class);
                        ObjectProvider<@NonNull RedisSentinelConfiguration> sentinelProvider = beanFactory.getBeanProvider(RedisSentinelConfiguration.class);
                        ObjectProvider<@NonNull RedisClusterConfiguration> clusterProvider = beanFactory.getBeanProvider(RedisClusterConfiguration.class);
                        ObjectProvider<@NonNull RedisStaticMasterReplicaConfiguration> masterReplicaProvider = beanFactory.getBeanProvider(RedisStaticMasterReplicaConfiguration.class);

                        try {
                            String connectionConfigurationClassName = isJedisConnectionFactory ? jedisConnectionConfigurationClassName : lettuceConnectionConfigurationClassName;
                            Class<?> clazz = ClassUtils.forName(connectionConfigurationClassName, ClassUtils.getDefaultClassLoader());
                            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
                            constructor.setAccessible(true);
                            Object configuration = constructor.newInstance(source,
                                    standaloneProvider,
                                    sentinelProvider,
                                    clusterProvider,
                                    masterReplicaProvider,
                                    connectionDetails);


                            if (isJedisConnectionFactory) {
                                ObjectProvider<@NonNull JedisClientConfigurationBuilderCustomizer> builderCustomizers = beanFactory.getBeanProvider(JedisClientConfigurationBuilderCustomizer.class);

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
                                ObjectProvider<@NonNull LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers = beanFactory.getBeanProvider(LettuceClientConfigurationBuilderCustomizer.class);
                                ObjectProvider<@NonNull LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers = beanFactory.getBeanProvider(LettuceClientOptionsBuilderCustomizer.class);
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
    Class<? extends MultiSourcesProperties<DataRedisProperties>> getMultiSourcesPropertiesClass() {
        return RedisMultiSourcesProperties.class;
    }

}
