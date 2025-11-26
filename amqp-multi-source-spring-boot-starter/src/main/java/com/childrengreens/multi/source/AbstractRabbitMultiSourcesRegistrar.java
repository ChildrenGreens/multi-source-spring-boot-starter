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

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;

import java.util.Objects;

/**
 * Abstract RabbitMQ multi-data-source BeanDefinition registrar.
 *
 * @author ChildrenGreens
 */
public abstract class AbstractRabbitMultiSourcesRegistrar extends AbstractMultiSourcesRegistrar<RabbitProperties> {

    /**
     * Get the ConnectionFactory based on the bean name prefix.
     * @param name prefix
     * @param beanFactory bean factory
     * @return connectionFactory
     */
    ConnectionFactory getConnectionFactoryBean(String name, ConfigurableListableBeanFactory beanFactory) {
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
