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

/**
 * RabbitMQ multi-data-source template BeanDefinition registrar.
 *
 * @author ChildrenGreens
 */
public class RabbitMultiSourcesTemplateRegistrar extends AbstractRabbitMultiSourcesRegistrar {
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
            Boolean isDynamic = environment.getProperty("spring.rabbitmq.dynamic", boolean.class, false);
            if (isDynamic){
                return;
            }

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


}
