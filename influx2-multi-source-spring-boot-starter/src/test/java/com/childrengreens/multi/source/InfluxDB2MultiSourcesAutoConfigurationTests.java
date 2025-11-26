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

import com.influxdb.client.InfluxDBClient;
import com.influxdb.spring.influx.InfluxDB2AutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class InfluxDB2MultiSourcesAutoConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    InfluxDB2AutoConfiguration.class,
                    InfluxDB2MultiSourcesAutoConfiguration.class
            ))
            .withPropertyValues(
                    "spring.multi-sources.influx.primary-key=alpha",
                    // alpha source
                    "spring.multi-sources.influx.sources.alpha.url=http://localhost:8086",
                    "spring.multi-sources.influx.sources.alpha.token=token-alpha",
                    "spring.multi-sources.influx.sources.alpha.org=my-org",
                    // beta source
                    "spring.multi-sources.influx.sources.beta.url=http://localhost:8087",
                    "spring.multi-sources.influx.sources.beta.token=token-beta",
                    "spring.multi-sources.influx.sources.beta.org=my-org"
            );

    @Test
    void registersMultipleInfluxClients() {
        this.contextRunner.run((context) -> {
            assertThat(context).hasBean("alphaInfluxDBClient");
            assertThat(context).hasBean("betaInfluxDBClient");

            assertThat(context.getBeansOfType(InfluxDBClient.class))
                    .containsKeys("alphaInfluxDBClient", "betaInfluxDBClient");
        });
    }
}

