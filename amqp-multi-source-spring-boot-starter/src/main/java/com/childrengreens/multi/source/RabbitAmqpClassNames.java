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

/**
 * Centralized RabbitMQ class names used for reflection/runtime hints.
 */
final class RabbitAmqpClassNames {

    static final String RABBIT_ANNOTATION_DRIVEN_CONFIGURATION = "org.springframework.boot.autoconfigure.amqp.RabbitAnnotationDrivenConfiguration";

    static final String PROPERTIES_RABBIT_CONNECTION_DETAILS = "org.springframework.boot.autoconfigure.amqp.PropertiesRabbitConnectionDetails";

    static final String SSL_BUNDLE_RABBIT_CONNECTION_FACTORY_BEAN = "org.springframework.boot.autoconfigure.amqp.SslBundleRabbitConnectionFactoryBean";

    private RabbitAmqpClassNames() {
    }
}
