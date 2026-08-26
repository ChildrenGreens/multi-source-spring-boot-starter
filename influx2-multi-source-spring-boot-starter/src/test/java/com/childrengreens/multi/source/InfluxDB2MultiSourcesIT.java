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
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.spring.health.InfluxDB2HealthIndicatorAutoConfiguration;
import com.influxdb.spring.influx.InfluxDB2AutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.health.contributor.CompositeHealthContributor;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.testcontainers.containers.InfluxDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class InfluxDB2MultiSourcesIT {

    private static final DockerImageName INFLUXDB_IMAGE = DockerImageName.parse("influxdb:2.7.11-alpine");

    private static final String ALPHA_ORG = "alpha-org";
    private static final String ALPHA_BUCKET = "alpha-bucket";
    private static final String ALPHA_TOKEN = "alpha-integration-test-token";
    private static final String BETA_ORG = "beta-org";
    private static final String BETA_BUCKET = "beta-bucket";
    private static final String BETA_TOKEN = "beta-integration-test-token";

    @Container
    private static final InfluxDBContainer<?> ALPHA_INFLUX = new InfluxDBContainer<>(INFLUXDB_IMAGE)
            .withUsername("alpha-user")
            .withPassword("alpha-password")
            .withOrganization(ALPHA_ORG)
            .withBucket(ALPHA_BUCKET)
            .withAdminToken(ALPHA_TOKEN);

    @Container
    private static final InfluxDBContainer<?> BETA_INFLUX = new InfluxDBContainer<>(INFLUXDB_IMAGE)
            .withUsername("beta-user")
            .withPassword("beta-password")
            .withOrganization(BETA_ORG)
            .withBucket(BETA_BUCKET)
            .withAdminToken(BETA_TOKEN);

    @Test
    void writesAndQueriesPointsThroughIndependentInfluxSources() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        InfluxDB2AutoConfiguration.class,
                        InfluxDB2MultiSourcesAutoConfiguration.class,
                        InfluxDB2HealthIndicatorAutoConfiguration.class
                ))
                .withPropertyValues(
                        "spring.multi-sources.influx.primary-key=alpha",
                        "spring.multi-sources.influx.sources.alpha.url=" + ALPHA_INFLUX.getUrl(),
                        "spring.multi-sources.influx.sources.alpha.token=" + ALPHA_TOKEN,
                        "spring.multi-sources.influx.sources.alpha.org=" + ALPHA_ORG,
                        "spring.multi-sources.influx.sources.alpha.bucket=" + ALPHA_BUCKET,
                        "spring.multi-sources.influx.sources.beta.url=" + BETA_INFLUX.getUrl(),
                        "spring.multi-sources.influx.sources.beta.token=" + BETA_TOKEN,
                        "spring.multi-sources.influx.sources.beta.org=" + BETA_ORG,
                        "spring.multi-sources.influx.sources.beta.bucket=" + BETA_BUCKET
                )
                .run((context) -> {
                    assertThat(context).hasNotFailed();
                    InfluxDBClient alpha = context.getBean("alphaInfluxDBClient", InfluxDBClient.class);
                    InfluxDBClient beta = context.getBean("betaInfluxDBClient", InfluxDBClient.class);

                    assertThat(alpha.ping()).isTrue();
                    assertThat(beta.ping()).isTrue();
                    assertThat(context.getBean(InfluxDBClient.class)).isSameAs(alpha);
                    assertThat(context.getBeanFactory()
                            .getBeanDefinition("alphaInfluxDBClient").isPrimary()).isTrue();
                    assertThat(context.getBeanFactory()
                            .getBeanDefinition("betaInfluxDBClient").isPrimary()).isFalse();

                    alpha.getWriteApiBlocking().writePoint(ALPHA_BUCKET, ALPHA_ORG, point(11L));
                    beta.getWriteApiBlocking().writePoint(BETA_BUCKET, BETA_ORG, point(22L));

                    assertThat(queryValues(alpha, ALPHA_BUCKET, ALPHA_ORG)).containsExactly(11L);
                    assertThat(queryValues(beta, BETA_BUCKET, BETA_ORG)).containsExactly(22L);

                    CompositeHealthContributor healthContributor = context.getBean(
                            "influxDbHealthContributor", CompositeHealthContributor.class);
                    assertHealthUp(healthContributor, "alphaInfluxDBClient");
                    assertHealthUp(healthContributor, "betaInfluxDBClient");
                });
    }

    private void assertHealthUp(CompositeHealthContributor contributor, String name) {
        assertThat(contributor.getContributor(name)).isInstanceOf(HealthIndicator.class);
        HealthIndicator indicator = (HealthIndicator) contributor.getContributor(name);
        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
    }

    private Point point(long value) {
        return Point.measurement("multi_source_integration_test")
                .addField("value", value)
                .time(Instant.now(), WritePrecision.NS);
    }

    private List<Long> queryValues(InfluxDBClient client, String bucket, String org) {
        String query = "from(bucket: \"" + bucket + "\")"
                + " |> range(start: -1h)"
                + " |> filter(fn: (r) => r._measurement == \"multi_source_integration_test\")"
                + " |> filter(fn: (r) => r._field == \"value\")";
        return client.getQueryApi().query(query, org).stream()
                .flatMap((table) -> table.getRecords().stream())
                .map(FluxRecord::getValue)
                .map(Number.class::cast)
                .map(Number::longValue)
                .toList();
    }
}
