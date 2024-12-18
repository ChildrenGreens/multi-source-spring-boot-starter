package com.childrengreens.multi.source;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.impl.CredentialsProvider;
import com.rabbitmq.client.impl.CredentialsRefreshService;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionNameStrategy;
import org.springframework.amqp.rabbit.connection.RabbitConnectionFactoryBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.amqp.*;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.core.io.ResourceLoader;


public class RabbitMultiSourcesConnectionFactoryRegistrar extends AbstractMultiSourcesRegistrar<RabbitProperties> {

    private static final String rabbitConnectionDetailsClassName = "org.springframework.boot.autoconfigure.amqp.PropertiesRabbitConnectionDetails";

    private static final String sslBundleRabbitConnectionFactoryBeanClassName = "org.springframework.boot.autoconfigure.amqp.SslBundleRabbitConnectionFactoryBean";

    @Override
    void registerBeanDefinitionsForSource(String name, RabbitProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            // register PropertiesRabbitConnectionDetails
            BeanDefinition rabbitConnectionDetailsBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RabbitConnectionDetails.class, () -> (RabbitConnectionDetails) newInstance(rabbitConnectionDetailsClassName,
                    new Class[]{RabbitProperties.class},
                    source)).getBeanDefinition();

            rabbitConnectionDetailsBeanDefinition.setPrimary(isPrimary);
            String rabbitConnectionDetailsBeanName = generateBeanName(rabbitConnectionDetailsClassName, name);
            registry.registerBeanDefinition(rabbitConnectionDetailsBeanName, rabbitConnectionDetailsBeanDefinition);

            // register RabbitConnectionFactoryBeanConfigurer
            BeanDefinition rabbitConnectionFactoryBeanConfigurerBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RabbitConnectionFactoryBeanConfigurer.class, () -> {

                ResourceLoader resourceLoader = beanFactory.getBean(ResourceLoader.class);
                RabbitConnectionDetails connectionDetails = beanFactory.getBean(rabbitConnectionDetailsBeanName, RabbitConnectionDetails.class);
                ObjectProvider<CredentialsProvider> credentialsProvider = beanFactory.getBeanProvider(CredentialsProvider.class);
                ObjectProvider<CredentialsRefreshService> credentialsRefreshService = beanFactory.getBeanProvider(CredentialsRefreshService.class);
                ObjectProvider<SslBundles> sslBundles = beanFactory.getBeanProvider(SslBundles.class);

                RabbitConnectionFactoryBeanConfigurer configurer = new RabbitConnectionFactoryBeanConfigurer(resourceLoader,
                        source, connectionDetails, sslBundles.getIfAvailable());
                configurer.setCredentialsProvider(credentialsProvider.getIfUnique());
                configurer.setCredentialsRefreshService(credentialsRefreshService.getIfUnique());
                return configurer;
            }).getBeanDefinition();
            rabbitConnectionFactoryBeanConfigurerBeanDefinition.setPrimary(isPrimary);
            String rabbitConnectionFactoryBeanConfigurerBeanName = generateBeanName(RabbitConnectionFactoryBeanConfigurer.class, name);
            registry.registerBeanDefinition(rabbitConnectionFactoryBeanConfigurerBeanName, rabbitConnectionFactoryBeanConfigurerBeanDefinition);

            // register CachingConnectionFactoryConfigurer
            BeanDefinition cachingConnectionFactoryConfigurerBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(CachingConnectionFactoryConfigurer.class, () -> {
                RabbitConnectionDetails rabbitConnectionDetails = beanFactory.getBean(rabbitConnectionDetailsBeanName, RabbitConnectionDetails.class);
                CachingConnectionFactoryConfigurer cachingConnectionFactoryConfigurer = new CachingConnectionFactoryConfigurer(source, rabbitConnectionDetails);
                ObjectProvider<ConnectionNameStrategy> connectionNameStrategy = beanFactory.getBeanProvider(ConnectionNameStrategy.class);
                cachingConnectionFactoryConfigurer.setConnectionNameStrategy(connectionNameStrategy.getIfUnique());
                return cachingConnectionFactoryConfigurer;
            }).getBeanDefinition();

            cachingConnectionFactoryConfigurerBeanDefinition.setPrimary(isPrimary);
            String cachingConnectionFactoryConfigurerBeanName = generateBeanName(CachingConnectionFactoryConfigurer.class, name);
            registry.registerBeanDefinition(cachingConnectionFactoryConfigurerBeanName, cachingConnectionFactoryConfigurerBeanDefinition);

            // register CachingConnectionFactory
            BeanDefinition cachingConnectionFactoryBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(CachingConnectionFactory.class, () -> {
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

            }).getBeanDefinition();
            cachingConnectionFactoryBeanDefinition.setPrimary(isPrimary);
            String cachingConnectionFactoryBeanName = generateBeanName(CachingConnectionFactory.class, name);
            registry.registerBeanDefinition(cachingConnectionFactoryBeanName, cachingConnectionFactoryBeanDefinition);
        }
    }


    @Override
    Class<? extends MultiSourcesProperties<RabbitProperties>> getMultiSourcesPropertiesClass() {
        return RabbitMultiSourcesProperties.class;
    }




}
