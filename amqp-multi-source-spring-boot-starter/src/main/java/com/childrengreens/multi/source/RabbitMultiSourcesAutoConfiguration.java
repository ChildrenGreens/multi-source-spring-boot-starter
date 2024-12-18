package com.childrengreens.multi.source;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration(before = RabbitAutoConfiguration.class)
@Import({RabbitMultiSourcesConnectionFactoryRegistrar.class, RabbitMultiSourcesTemplateRegistrar.class, RabbitMultiSourcesAnnotationDrivenRegistrar.class})
public class RabbitMultiSourcesAutoConfiguration {
}
