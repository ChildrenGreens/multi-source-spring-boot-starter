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

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.amqp.autoconfigure.*;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;

/**
 * RabbitMQ multi-data-source {@link CachingConnectionFactory} BeanDefinition registrar.
 *
 * @author ChildrenGreens
 */
public class RabbitMultiSourcesConnectionFactoryRegistrar extends AbstractRabbitMultiSourcesRegistrar implements ResourceLoaderAware {
    private ResourceLoader resourceLoader;

    private static final String rabbitConnectionDetailsClassName = "org.springframework.boot.amqp.autoconfigure.PropertiesRabbitConnectionDetails";

    private static final String sslBundleRabbitConnectionFactoryBeanClassName = "org.springframework.boot.amqp.autoconfigure.SslBundleRabbitConnectionFactoryBean";

    @Override
    void registerBeanDefinitionsForSource(String name, RabbitProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            // register PropertiesRabbitConnectionDetails
            String rabbitConnectionDetailsBeanName = generateBeanName(rabbitConnectionDetailsClassName, name);
            registerBeanDefinition(registry,
                    RabbitConnectionDetails.class,
                    rabbitConnectionDetailsBeanName,
                    isPrimary,
                    () -> {
                        ObjectProvider<SslBundles> sslBundles = beanFactory.getBeanProvider(SslBundles.class);
                        return (RabbitConnectionDetails) newInstance(rabbitConnectionDetailsClassName, new Class[]{RabbitProperties.class, SslBundles.class}, source, sslBundles.getIfAvailable());
                    });

            // register RabbitConnectionFactoryBeanConfigurer
            String rabbitConnectionFactoryBeanConfigurerBeanName = generateBeanName(RabbitConnectionFactoryBeanConfigurer.class, name);
            registerBeanDefinition(registry,
                    RabbitConnectionFactoryBeanConfigurer.class,
                    rabbitConnectionFactoryBeanConfigurerBeanName,
                    isPrimary,
                    () -> {
                        RabbitConnectionDetails connectionDetails = beanFactory.getBean(rabbitConnectionDetailsBeanName, RabbitConnectionDetails.class);
                        ObjectProvider<CredentialsProvider> credentialsProvider = beanFactory.getBeanProvider(CredentialsProvider.class);
                        ObjectProvider<CredentialsRefreshService> credentialsRefreshService = beanFactory.getBeanProvider(CredentialsRefreshService.class);

                        RabbitConnectionFactoryBeanConfigurer configurer = new RabbitConnectionFactoryBeanConfigurer(resourceLoader,
                                source, connectionDetails);
                        configurer.setCredentialsProvider(credentialsProvider.getIfUnique());
                        configurer.setCredentialsRefreshService(credentialsRefreshService.getIfUnique());
                        return configurer;
                    });

            // register CachingConnectionFactoryConfigurer
            String cachingConnectionFactoryConfigurerBeanName = generateBeanName(CachingConnectionFactoryConfigurer.class, name);
            registerBeanDefinition(registry,
                    CachingConnectionFactoryConfigurer.class,
                    cachingConnectionFactoryConfigurerBeanName,
                    isPrimary,
                    () -> {
                        RabbitConnectionDetails rabbitConnectionDetails = beanFactory.getBean(rabbitConnectionDetailsBeanName, RabbitConnectionDetails.class);
                        CachingConnectionFactoryConfigurer configurer = new CachingConnectionFactoryConfigurer(source, rabbitConnectionDetails);
                        ObjectProvider<ConnectionNameStrategy> connectionNameStrategy = beanFactory.getBeanProvider(ConnectionNameStrategy.class);
                        configurer.setConnectionNameStrategy(connectionNameStrategy.getIfUnique());
                        return configurer;
                    });

            // register CachingConnectionFactory
            registerBeanDefinition(registry,
                    CachingConnectionFactory.class,
                    generateBeanName(CachingConnectionFactory.class, name),
                    isPrimary,
                    () -> {
                        RabbitConnectionFactoryBeanConfigurer rabbitConnectionFactoryBeanConfigurer = beanFactory.getBean(rabbitConnectionFactoryBeanConfigurerBeanName, RabbitConnectionFactoryBeanConfigurer.class);
                        CachingConnectionFactoryConfigurer rabbitCachingConnectionFactoryConfigurer = beanFactory.getBean(cachingConnectionFactoryConfigurerBeanName, CachingConnectionFactoryConfigurer.class);
                        ObjectProvider<ConnectionFactoryCustomizer> connectionFactoryCustomizers = beanFactory.getBeanProvider(ConnectionFactoryCustomizer.class);

                        RabbitConnectionFactoryBean connectionFactoryBean = (RabbitConnectionFactoryBean) newInstance(sslBundleRabbitConnectionFactoryBeanClassName, null);
                        rabbitConnectionFactoryBeanConfigurer.configure(connectionFactoryBean);
                        connectionFactoryBean.afterPropertiesSet();
                        try {
                            ConnectionFactory connectionFactory = connectionFactoryBean.getObject();
                            connectionFactoryCustomizers.orderedStream().forEach((customizer) -> customizer.customize(connectionFactory));
                            CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory(connectionFactory);
                            rabbitCachingConnectionFactoryConfigurer.configure(cachingConnectionFactory);
                            return cachingConnectionFactory;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }

                    });
        }
    }


    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }
}
