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

import org.springframework.amqp.rabbit.config.ContainerCustomizer;
import org.springframework.amqp.rabbit.config.DirectRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.amqp.DirectRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.ResolvableType;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * RabbitMQ multi-data-source annotation-driven BeanDefinition registrar.
 *
 * @author ChildrenGreens
 */
public class RabbitMultiSourcesAnnotationDrivenRegistrar extends AbstractRabbitMultiSourcesRegistrar {

    @Override
    void registerBeanDefinitionsForSource(String name, RabbitProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {


        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            try {
                Class<?> clazz = ClassUtils.forName(RabbitAmqpClassNames.RABBIT_ANNOTATION_DRIVEN_CONFIGURATION, ClassUtils.getDefaultClassLoader());
                Method simpleListenerConfigurer = clazz.getDeclaredMethod("simpleListenerConfigurer");
                Method directListenerConfigurer = clazz.getDeclaredMethod("directListenerConfigurer");
                RabbitProperties.ContainerType type = source.getListener().getType();

                switch (type) {
                    case SIMPLE -> {

                        String simpleConfigurerBeanName = generateBeanName(SimpleRabbitListenerContainerFactoryConfigurer.class, name);
                        // register SimpleRabbitListenerContainerFactoryConfigurer
                        registerBeanDefinition(registry,
                                SimpleRabbitListenerContainerFactoryConfigurer.class,
                                simpleConfigurerBeanName,
                                isPrimary,
                                ()-> {
                                    Object bean = beanFactory.getBean(clazz);

                                    simpleListenerConfigurer.setAccessible(true);
                                    try {
                                        SimpleRabbitListenerContainerFactoryConfigurer configurer = (SimpleRabbitListenerContainerFactoryConfigurer) simpleListenerConfigurer.invoke(bean);
                                        if (isVirtualThreads()) {
                                            configurer.setTaskExecutor(new VirtualThreadTaskExecutor("rabbit-simple-"));
                                        }
                                        return configurer;
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw new RuntimeException(e);
                                    }
                                });


                        // register SimpleRabbitListenerContainerFactory
                        registerBeanDefinition(registry,
                                SimpleRabbitListenerContainerFactory.class,
                                generateBeanName(SimpleRabbitListenerContainerFactory.class, name),
                                isPrimary,
                                ()-> {
                                    SimpleRabbitListenerContainerFactoryConfigurer configurer = beanFactory.getBean(simpleConfigurerBeanName, SimpleRabbitListenerContainerFactoryConfigurer.class);
                                    ResolvableType resolvableType = ResolvableType.forType(new ParameterizedTypeReference<ContainerCustomizer<SimpleMessageListenerContainer>>() {
                                    });
                                    ObjectProvider<ContainerCustomizer<SimpleMessageListenerContainer>> simpleContainerCustomizer = beanFactory.getBeanProvider(resolvableType);
                                    ConnectionFactory connectionFactory = getConnectionFactoryBean(name, beanFactory);

                                    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
                                    configurer.configure(factory, connectionFactory);
                                    simpleContainerCustomizer.ifUnique(factory::setContainerCustomizer);
                                    return factory;
                                });


                    }
                    case DIRECT -> {

                        String directConfigurerBeanName = generateBeanName(DirectRabbitListenerContainerFactoryConfigurer.class, name);
                        // register DirectRabbitListenerContainerFactoryConfigurer
                        registerBeanDefinition(registry,
                                DirectRabbitListenerContainerFactoryConfigurer.class,
                                directConfigurerBeanName,
                                isPrimary,
                                ()-> {
                                    Object bean = beanFactory.getBean(clazz);

                                    directListenerConfigurer.setAccessible(true);
                                    try {
                                        DirectRabbitListenerContainerFactoryConfigurer configurer = (DirectRabbitListenerContainerFactoryConfigurer) directListenerConfigurer.invoke(bean);
                                        if (isVirtualThreads()) {
                                            configurer.setTaskExecutor(new VirtualThreadTaskExecutor("rabbit-direct-"));
                                        }
                                        return configurer;
                                    } catch (IllegalAccessException | InvocationTargetException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                        // register DirectRabbitListenerContainerFactory
                        registerBeanDefinition(registry,
                                DirectRabbitListenerContainerFactory.class,
                                generateBeanName(DirectRabbitListenerContainerFactory.class, name),
                                isPrimary,
                                ()-> {
                                    DirectRabbitListenerContainerFactoryConfigurer configurer = beanFactory.getBean(directConfigurerBeanName, DirectRabbitListenerContainerFactoryConfigurer.class);
                                    ResolvableType resolvableType = ResolvableType.forType(new ParameterizedTypeReference<ContainerCustomizer<DirectMessageListenerContainer>>() {
                                    });
                                    ObjectProvider<ContainerCustomizer<DirectMessageListenerContainer>> directContainerCustomizer = beanFactory.getBeanProvider(resolvableType);
                                    ConnectionFactory connectionFactory = getConnectionFactoryBean(name, beanFactory);

                                    DirectRabbitListenerContainerFactory factory = new DirectRabbitListenerContainerFactory();
                                    configurer.configure(factory, connectionFactory);
                                    directContainerCustomizer.ifUnique(factory::setContainerCustomizer);
                                    return factory;
                                });


                    }
                }
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
