package com.childrengreens.multi.source;

import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.RabbitRetryTemplateCustomizer;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitTemplateCustomizer;

import java.util.Objects;

public class RabbitMultiSourcesTemplateRegistrar extends AbstractMultiSourcesRegistrar<RabbitProperties> {
    @Override
    void registerBeanDefinitionsForSource(String name, RabbitProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {
        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {

            // register RabbitTemplateConfigurer
            String rabbitTemplateConfigurerBeanName = generateBeanName(RabbitTemplateConfigurer.class, name);

            registerBeanDefinition(registry,
                    RabbitTemplateConfigurer.class,
                    rabbitTemplateConfigurerBeanName,
                    isPrimary,
                    ()-> {
                        ObjectProvider<MessageConverter> messageConverter = beanFactory.getBeanProvider(MessageConverter.class);
                        ObjectProvider<RabbitRetryTemplateCustomizer> retryTemplateCustomizers = beanFactory.getBeanProvider(RabbitRetryTemplateCustomizer.class);

                        RabbitTemplateConfigurer configurer = new RabbitTemplateConfigurer(source);
                        configurer.setMessageConverter(messageConverter.getIfAvailable());
                        configurer.setRetryTemplateCustomizers(retryTemplateCustomizers.orderedStream().toList());
                        return configurer;
                    });

            // register RabbitTemplate
            String rabbitTemplateBeanName = generateBeanName(RabbitTemplate.class, name);
            registerBeanDefinition(registry,
                    RabbitTemplate.class,
                    rabbitTemplateBeanName,
                    isPrimary,
                    () -> {
                        RabbitTemplateConfigurer configurer = beanFactory.getBean(rabbitTemplateConfigurerBeanName, RabbitTemplateConfigurer.class);
                        ConnectionFactory connectionFactory = getConnectionFactoryBean(name, beanFactory);
                        ObjectProvider<RabbitTemplateCustomizer> customizers = beanFactory.getBeanProvider(RabbitTemplateCustomizer.class);


                        RabbitTemplate template = new RabbitTemplate();
                        configurer.configure(template, connectionFactory);
                        customizers.orderedStream().forEach((customizer) -> customizer.customize(template));
                        return template;
                    });

            // register RabbitMessagingTemplate
            registerBeanDefinition(registry,
                    RabbitMessagingTemplate.class,
                    generateBeanName(RabbitMessagingTemplate.class, name),
                    isPrimary,
                    () -> {
                        RabbitTemplate rabbitTemplate = beanFactory.getBean(rabbitTemplateBeanName, RabbitTemplate.class);
                        return new RabbitMessagingTemplate(rabbitTemplate);
                    });

            // register AmqpAdmin
            registerBeanDefinition(registry,
                    AmqpAdmin.class,
                    generateBeanName(RabbitTemplate.class, name),
                    isPrimary,
                    () -> {
                        ConnectionFactory connectionFactory = getConnectionFactoryBean(name, beanFactory);
                        return new RabbitAdmin(connectionFactory);
                    });

        }
    }

    private ConnectionFactory getConnectionFactoryBean(String name, ConfigurableListableBeanFactory beanFactory) {
        String[] beanNamesForType = beanFactory.getBeanNamesForType(ConnectionFactory.class);
        ConnectionFactory connectionFactory = null;
        for (String beanName : beanNamesForType) {
            if (beanName.startsWith(name)) {
                connectionFactory = beanFactory.getBean(beanName, ConnectionFactory.class);
            }
        }
        if (Objects.isNull(connectionFactory)) {
            throw new RuntimeException("source key: " + name + ", " + "RabbitTemplate connection factory not found");
        }
        return connectionFactory;
    }

    @Override
    Class<? extends MultiSourcesProperties<RabbitProperties>> getMultiSourcesPropertiesClass() {
        return RabbitMultiSourcesProperties.class;
    }
}
