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

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Abstract multi-data-source BeanDefinition registrar.
 *
 * @author ChildrenGreens
 */
public abstract class AbstractMultiSourcesRegistrar<D> implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    protected Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        Class<MultiSourcesProperties<D>> clazz = (Class<MultiSourcesProperties<D>>) getMultiSourcesPropertiesClass();

        ConfigurationProperties annotation = clazz.getAnnotation(ConfigurationProperties.class);
        BindResult<MultiSourcesProperties<D>> bind = Binder.get(environment).bind(annotation.prefix(), clazz);

        MultiSourcesProperties<D> multiSourcesProperties = bind.get();

        if (Objects.isNull(multiSourcesProperties) || CollectionUtils.isEmpty(multiSourcesProperties.getSources())) {
            return;
        }

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            multiSourcesProperties.getSources().forEach((name, source ) -> {

                // Whether it is Primary
                boolean isPrimary = name.equals(multiSourcesProperties.getPrimaryKey());

                // register
                registerBeanDefinitionsForSource(name, source, registry, isPrimary);
            });
        }
    }

    /**
     * Whether it is virtual threads.
     * @return bool
     */
    Boolean isVirtualThreads() {
        return Threading.VIRTUAL.isActive(environment);
    }


    /**
     * Generate bean names using the class name and prefix.
     * @param className class name
     * @param prefix prefix
     * @return bean name
     */
    protected String generateBeanName(String className, String prefix) {
        try {
            Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            return generateBeanName(clazz, prefix);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Generate bean names using the class objects and prefix.
     * @param clazz class objects
     * @param prefix prefix
     * @return bean name
     */
    protected String generateBeanName(Class<?> clazz, String prefix) {
        return prefix + clazz.getSimpleName();
    }


    /**
     * Create an instance using a constructor.
     * @param className class objects.
     * @param parameterTypes the parameter array.
     * @param args array of objects to be passed as arguments to the constructor call.
     * @return a new object created by calling the constructor this object represents.
     */
    protected Object newInstance(String className, Class<?>[] parameterTypes, Object... args) {
        try {
            Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
            Class<?>[] resolvedParameterTypes = (parameterTypes != null ? parameterTypes : new Class<?>[0]);
            Constructor<?> declaredConstructor = clazz.getDeclaredConstructor(resolvedParameterTypes);
            declaredConstructor.setAccessible(true);
            return declaredConstructor.newInstance(args);
        } catch (ClassNotFoundException | InvocationTargetException | NoSuchMethodException | InstantiationException |
                 IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Register bean definition
     * @param registry current bean definition registry.
     * @param clazz class
     * @param beanName bean name
     * @param isPrimary determine whether the current datasource is primary.
     * @param instanceSupplier a callback for creating an instance of the bean
     * @param <T> T
     */
    protected <T> void registerBeanDefinition(BeanDefinitionRegistry registry, Class<T> clazz, String beanName, Boolean isPrimary, Supplier<T> instanceSupplier) {
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(clazz, instanceSupplier).getBeanDefinition();
        beanDefinition.setPrimary(isPrimary);
        registry.registerBeanDefinition(beanName, beanDefinition);
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


    /**
     * Register multiple beans in a single datasource.
     * @param name source name.
     * @param source source configuration.
     * @param registry current bean definition registry.
     * @param isPrimary determine whether the current datasource is primary.
     */
    abstract void registerBeanDefinitionsForSource(String name, D source, BeanDefinitionRegistry registry, Boolean isPrimary);


    /**
     * Obtain multiple datasource Properties class objects.
     * @return class objects.
     */
    abstract Class<? extends MultiSourcesProperties<D>> getMultiSourcesPropertiesClass();

}
