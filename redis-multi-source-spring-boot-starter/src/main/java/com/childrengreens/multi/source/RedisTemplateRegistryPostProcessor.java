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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;

/**
 * Create a corresponding {@link RedisTemplate} and {@link StringRedisTemplate} based on the {@link RedisConnectionFactory} bean.
 *
 * @author ChildrenGreens
 */
public class RedisTemplateRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(@NonNull BeanDefinitionRegistry registry) throws BeansException {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            String[] beanNames = beanFactory.getBeanNamesForType(RedisConnectionFactory.class);

            for (String beanName : beanNames) {
                // Whether it is Primary
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                boolean primary = bd.isPrimary();

                // suffix
                String suffix;
                if (beanName.endsWith(LettuceConnectionFactory.class.getSimpleName())) {
                    suffix = LettuceConnectionFactory.class.getSimpleName();
                } else {
                    suffix = JedisConnectionFactory.class.getSimpleName();
                }

                // Create a corresponding RedisTemplate based on the RedisConnectionFactory bean.
                BeanDefinition redisTemplateBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RedisTemplate.class, () -> {
                    RedisConnectionFactory factory = beanFactory.getBean(beanName, RedisConnectionFactory.class);
                    RedisTemplate<Object, Object> template = new RedisTemplate<>();
                    template.setConnectionFactory(factory);
                    return template;
                }).getBeanDefinition();

                String redisTemplateBeanName = beanName.replace(suffix, RedisTemplate.class.getSimpleName());
                redisTemplateBeanDefinition.setPrimary(primary);
                registry.registerBeanDefinition(redisTemplateBeanName, redisTemplateBeanDefinition);

                // Create a corresponding StringRedisTemplate based on the RedisConnectionFactory bean.
                BeanDefinition stringRedisTemplateBeanDefinition = BeanDefinitionBuilder.genericBeanDefinition(StringRedisTemplate.class, () -> {
                    RedisConnectionFactory factory = beanFactory.getBean(beanName, RedisConnectionFactory.class);
                    return new StringRedisTemplate(factory);
                }).getBeanDefinition();

                String stringRedisTemplateBeanName = beanName.replace(suffix, StringRedisTemplate.class.getSimpleName());
                stringRedisTemplateBeanDefinition.setPrimary(primary);
                registry.registerBeanDefinition(stringRedisTemplateBeanName, stringRedisTemplateBeanDefinition);
            }
        }

    }

}
