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

/**
 * Create a corresponding {@link RedisTemplate} and {@link StringRedisTemplate} based on the {@link RedisConnectionFactory} bean.
 *
 * @author ChildrenGreens
 */
public class RedisTemplateRegistryPostProcessor implements BeanDefinitionRegistryPostProcessor {
    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {
            String[] beanNames = beanFactory.getBeanNamesForType(RedisConnectionFactory.class);

            for (String beanName : beanNames) {
                // Whether it is Primary
                BeanDefinition bd = beanFactory.getBeanDefinition(beanName);
                boolean primary = bd.isPrimary();

                // suffix
                String suffix = null;
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
