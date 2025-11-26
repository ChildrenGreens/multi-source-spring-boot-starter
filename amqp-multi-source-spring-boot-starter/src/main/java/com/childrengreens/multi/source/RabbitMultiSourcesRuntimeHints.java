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

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.autoconfigure.amqp.RabbitProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Runtime hints for reflective RabbitMQ infrastructure access.
 *
 * @author ChildrenGreens
 */
public class RabbitMultiSourcesRuntimeHints implements RuntimeHintsRegistrar {

    private static final String PROPERTIES_RABBIT_CONNECTION_DETAILS = "org.springframework.boot.autoconfigure.amqp.PropertiesRabbitConnectionDetails";
    private static final String SSL_BUNDLE_RABBIT_CONNECTION_FACTORY_BEAN = "org.springframework.boot.autoconfigure.amqp.SslBundleRabbitConnectionFactoryBean";
    private static final String RABBIT_ANNOTATION_DRIVEN_CONFIGURATION = "org.springframework.boot.autoconfigure.amqp.RabbitAnnotationDrivenConfiguration";

    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        registerConnectionDetails(hints, classLoader);
        registerSslBundleConnectionFactoryBean(hints, classLoader);
        registerAnnotationDrivenConfiguration(hints, classLoader);
    }

    private void registerConnectionDetails(RuntimeHints hints, ClassLoader classLoader) {
        try {
            Class<?> connectionDetails = resolveClass(PROPERTIES_RABBIT_CONNECTION_DETAILS, classLoader);
            Constructor<?> constructor = connectionDetails.getDeclaredConstructor(RabbitProperties.class, SslBundles.class);
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to register Rabbit connection details constructor hint", ex);
        }
    }

    private void registerSslBundleConnectionFactoryBean(RuntimeHints hints, ClassLoader classLoader) {
        try {
            Class<?> connectionFactoryBean = resolveClass(SSL_BUNDLE_RABBIT_CONNECTION_FACTORY_BEAN, classLoader);
            Constructor<?> constructor = connectionFactoryBean.getDeclaredConstructor();
            hints.reflection().registerConstructor(constructor, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to register Rabbit connection factory bean constructor hint", ex);
        }
    }

    private void registerAnnotationDrivenConfiguration(RuntimeHints hints, ClassLoader classLoader) {
        try {
            Class<?> annotationDrivenConfiguration = resolveClass(RABBIT_ANNOTATION_DRIVEN_CONFIGURATION, classLoader);
            Method simpleConfigurer = annotationDrivenConfiguration.getDeclaredMethod("simpleListenerConfigurer");
            Method directConfigurer = annotationDrivenConfiguration.getDeclaredMethod("directListenerConfigurer");
            hints.reflection().registerMethod(simpleConfigurer, ExecutableMode.INVOKE);
            hints.reflection().registerMethod(directConfigurer, ExecutableMode.INVOKE);
        } catch (NoSuchMethodException ex) {
            throw new IllegalStateException("Failed to register Rabbit listener configuration hints", ex);
        }
    }

    private Class<?> resolveClass(String className, ClassLoader classLoader) {
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Failed to resolve class for runtime hints: " + className, ex);
        }
    }
}
