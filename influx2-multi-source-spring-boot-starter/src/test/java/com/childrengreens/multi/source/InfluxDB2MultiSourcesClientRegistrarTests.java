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

import com.influxdb.spring.influx.InfluxDB2Properties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class InfluxDB2MultiSourcesClientRegistrarTests {

    @Test
    void ignoresRegistryThatIsNotAConfigurableBeanFactory() {
        InfluxDB2MultiSourcesClientRegistrar registrar = new InfluxDB2MultiSourcesClientRegistrar();
        SimpleBeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();

        registrar.registerBeanDefinitionsForSource("alpha", new InfluxDB2Properties(), registry, true);

        assertThat(registry.getBeanDefinitionCount()).isZero();
    }
}
