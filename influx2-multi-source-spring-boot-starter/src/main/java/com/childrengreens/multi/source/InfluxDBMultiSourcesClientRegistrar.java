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

import com.influxdb.client.InfluxDBClient;
import com.influxdb.spring.influx.InfluxDB2AutoConfiguration;
import com.influxdb.spring.influx.InfluxDB2OkHttpClientBuilderProvider;
import com.influxdb.spring.influx.InfluxDB2Properties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Dynamically create multiple {@link InfluxDBClient} based on Environment.
 *
 * @author ChildrenGreens
 */
public class InfluxDBMultiSourcesClientRegistrar extends AbstractMultiSourcesRegistrar<InfluxDB2Properties> {
    @Override
    void registerBeanDefinitionsForSource(String name, InfluxDB2Properties source, BeanDefinitionRegistry registry, Boolean isPrimary) {
        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {

            registerBeanDefinition(registry,
                    InfluxDBClient.class,
                    generateBeanName(InfluxDBClient.class, name),
                    isPrimary,
                    () -> {
                        ObjectProvider<InfluxDB2OkHttpClientBuilderProvider> builderProvider = beanFactory.getBeanProvider(InfluxDB2OkHttpClientBuilderProvider.class);
                        InfluxDB2AutoConfiguration influxDB2AutoConfiguration = new InfluxDB2AutoConfiguration(source, builderProvider);
                        return influxDB2AutoConfiguration.influxDBClient();
                    });

        }
    }


    @Override
    Class<? extends MultiSourcesProperties<InfluxDB2Properties>> getMultiSourcesPropertiesClass() {
        return InfluxDB2MultiSourcesProperties.class;
    }
}
