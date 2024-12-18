package com.childrengreens.multi.source;

import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.multi-sources.rabbitmq")
public class RabbitMultiSourcesProperties extends MultiSourcesProperties<RabbitProperties> {
}
