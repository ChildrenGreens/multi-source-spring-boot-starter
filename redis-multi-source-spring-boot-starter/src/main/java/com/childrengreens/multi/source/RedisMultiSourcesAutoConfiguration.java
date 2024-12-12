package com.childrengreens.multi.source;

import io.lettuce.core.resource.DefaultClientResources;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.ClientResourcesBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;


/**
 * {@link EnableAutoConfiguration Auto-configuration} for Spring Data's multiple Redis data sources support.
 *
 * @author ChildrenGreens
 */

@AutoConfiguration(before = RedisAutoConfiguration.class)
@Import({LettuceConnectionMultiSourcesRegistrar.class, RedisTemplateRegistryPostProcessor.class})
public class RedisMultiSourcesAutoConfiguration {

    @Bean(destroyMethod = "shutdown")
    DefaultClientResources lettuceClientResources(ObjectProvider<ClientResourcesBuilderCustomizer> customizers) {
        DefaultClientResources.Builder builder = DefaultClientResources.builder();
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }


}
