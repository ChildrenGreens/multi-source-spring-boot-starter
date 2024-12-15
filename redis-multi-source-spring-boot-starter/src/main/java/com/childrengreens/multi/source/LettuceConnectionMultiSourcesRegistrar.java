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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

/**
 * Dynamically create multiple {@link LettuceConnectionFactory} based on Environment.
 *
 * @author ChildrenGreens
 */
public class LettuceConnectionMultiSourcesRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        ConfigurationProperties annotation = RedisMultiSourcesProperties.class.getAnnotation(ConfigurationProperties.class);
        BindResult<RedisMultiSourcesProperties> bind = Binder.get(environment).bind(annotation.prefix(), RedisMultiSourcesProperties.class);
        RedisMultiSourcesProperties redisMultiSourcesProperties = bind.get();

        if (Objects.isNull(redisMultiSourcesProperties) || CollectionUtils.isEmpty(redisMultiSourcesProperties.getSources())) {
            return;
        }

        if (registry instanceof ConfigurableListableBeanFactory) {

            ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) registry;

            redisMultiSourcesProperties.getSources().forEach((name, source) -> {

                // Whether it is Primary
                boolean isPrimary = name.equals(redisMultiSourcesProperties.getPrimaryKey());

                // LettuceConnectionFactory registration
                BeanDefinition connectionFactoryBD = BeanDefinitionBuilder.genericBeanDefinition(LettuceConnectionFactory.class, () -> {

                    // new LettuceMultiSourcesConnectionConfiguration
                    ObjectProvider<RedisStandaloneConfiguration> standaloneProvider = beanFactory.getBeanProvider(RedisStandaloneConfiguration.class);
                    ObjectProvider<RedisSentinelConfiguration> sentinelProvider = beanFactory.getBeanProvider(RedisSentinelConfiguration.class);
                    ObjectProvider<RedisClusterConfiguration> clusterProvider = beanFactory.getBeanProvider(RedisClusterConfiguration.class);

                    LettuceMultiSourcesConnectionConfiguration lettuceMultiSourcesConnectionConfiguration = new LettuceMultiSourcesConnectionConfiguration(source,
                            standaloneProvider,
                            sentinelProvider,
                            clusterProvider);

                    ObjectProvider<LettuceClientConfigurationBuilderCustomizer> configurationBuilderCustomizerProvider = beanFactory.getBeanProvider(LettuceClientConfigurationBuilderCustomizer.class);
                    ClientResources clientResources = beanFactory.getBean(ClientResources.class);

                    LettuceClientConfiguration clientConfig = lettuceMultiSourcesConnectionConfiguration.getLettuceClientConfiguration(configurationBuilderCustomizerProvider,
                            clientResources,
                            source.getLettuce().getPool());

                    return lettuceMultiSourcesConnectionConfiguration.createLettuceConnectionFactory(clientConfig);

                }).getBeanDefinition();

                connectionFactoryBD.setPrimary(isPrimary);
                String connectionFactoryBDN = name + LettuceConnectionFactory.class.getSimpleName();
                registry.registerBeanDefinition(connectionFactoryBDN, connectionFactoryBD);
            });

        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


}
