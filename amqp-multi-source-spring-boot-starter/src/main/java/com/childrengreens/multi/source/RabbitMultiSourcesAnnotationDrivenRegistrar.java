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
import java.util.Objects;

public class RabbitMultiSourcesAnnotationDrivenRegistrar extends AbstractMultiSourcesRegistrar<RabbitProperties> {

    private static final String rabbitAnnotationDrivenConfigurationClassName = "org.springframework.boot.autoconfigure.amqp.RabbitAnnotationDrivenConfiguration";


    @Override
    void registerBeanDefinitionsForSource(String name, RabbitProperties source, BeanDefinitionRegistry registry, Boolean isPrimary) {


        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            try {
                Class<?> clazz = ClassUtils.forName(rabbitAnnotationDrivenConfigurationClassName, ClassUtils.getDefaultClassLoader());
                Method simpleListenerConfigurer = ClassUtils.getMethod(clazz, "simpleListenerConfigurer");
                Method directListenerConfigurer = ClassUtils.getMethod(clazz, "directListenerConfigurer");

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
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }




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
