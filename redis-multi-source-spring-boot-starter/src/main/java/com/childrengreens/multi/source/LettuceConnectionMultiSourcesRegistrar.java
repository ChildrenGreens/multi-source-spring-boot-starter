/*
 * Copyright 2012-2024 the original author or authors.
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
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Dynamically create multiple {@link RedisConnectionDetails} and {@link LettuceConnectionFactory} based on Environment.
 *
 * @author ChildrenGreens
 */
public class LettuceConnectionMultiSourcesRegistrar extends AbstractMultiSourcesRegistrar<RedisProperties> {


    private static final String redisConnectionDetailsClassName = "org.springframework.boot.autoconfigure.data.redis.PropertiesRedisConnectionDetails";

    private static final String lettuceConnectionConfigurationClassName = "org.springframework.boot.autoconfigure.data.redis.LettuceConnectionConfiguration";

    @Override
    void registerBeanDefinitionsForSource(String name, RedisProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            // register PropertiesRedisConnectionDetails
            String redisConnectionDetailsBeanName = generateBeanName(redisConnectionDetailsClassName, name);
            registerBeanDefinition(registry,
                    RedisConnectionDetails.class,
                    redisConnectionDetailsBeanName,
                    isPrimary,
                    () -> (RedisConnectionDetails) newInstance(redisConnectionDetailsClassName, new Class[]{RedisProperties.class}, source));


            // register LettuceConnectionFactory
            registerBeanDefinition(registry,
                    LettuceConnectionFactory.class,
                    generateBeanName(LettuceConnectionFactory.class, name),
                    isPrimary,
                    () -> {
                        // new LettuceConnectionConfiguration
                        RedisConnectionDetails connectionDetails = beanFactory.getBean(redisConnectionDetailsBeanName, RedisConnectionDetails.class);
                        ObjectProvider<RedisStandaloneConfiguration> standaloneProvider = beanFactory.getBeanProvider(RedisStandaloneConfiguration.class);
                        ObjectProvider<RedisSentinelConfiguration> sentinelProvider = beanFactory.getBeanProvider(RedisSentinelConfiguration.class);
                        ObjectProvider<RedisClusterConfiguration> clusterProvider = beanFactory.getBeanProvider(RedisClusterConfiguration.class);
                        ObjectProvider<SslBundles> sslBundlesProvider = beanFactory.getBeanProvider(SslBundles.class);

                        try {
                            Class<?> clazz = ClassUtils.forName(lettuceConnectionConfigurationClassName, ClassUtils.getDefaultClassLoader());
                            Constructor<?> constructor = clazz.getDeclaredConstructors()[0];
                            Method createConnectionFactory = clazz.getDeclaredMethod("createConnectionFactory", ObjectProvider.class, ObjectProvider.class, ClientResources.class);
                            constructor.setAccessible(true);
                            Object configuration = constructor.newInstance(source,
                                    standaloneProvider,
                                    sentinelProvider,
                                    clusterProvider,
                                    connectionDetails,
                                    sslBundlesProvider);


                            ObjectProvider<LettuceClientConfigurationBuilderCustomizer> clientConfigurationBuilderCustomizers = beanFactory.getBeanProvider(LettuceClientConfigurationBuilderCustomizer.class);
                            ObjectProvider<LettuceClientOptionsBuilderCustomizer> clientOptionsBuilderCustomizers = beanFactory.getBeanProvider(LettuceClientOptionsBuilderCustomizer.class);
                            ClientResources clientResources = beanFactory.getBean(ClientResources.class);

                            createConnectionFactory.setAccessible(true);
                            LettuceConnectionFactory factory = (LettuceConnectionFactory) createConnectionFactory.invoke(configuration, clientConfigurationBuilderCustomizers, clientOptionsBuilderCustomizers, clientResources);

                            if (isVirtualThreads()) {
                                SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("redis-");
                                executor.setVirtualThreads(true);
                                factory.setExecutor(executor);
                            }
                            return factory;
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
