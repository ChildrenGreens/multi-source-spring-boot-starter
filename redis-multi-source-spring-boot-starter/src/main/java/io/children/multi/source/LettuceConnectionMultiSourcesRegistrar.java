package io.children.multi.source;

import io.lettuce.core.resource.ClientResources;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientOptionsBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.thread.Threading;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.BindResult;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.util.CollectionUtils;

import java.util.Objects;

/**
 * Dynamically create multiple {@link RedisConnectionDetails} and {@link LettuceConnectionFactory} based on Environment.
 *
 * @author ChildrenGreens
 */
public class LettuceConnectionMultiSourcesRegistrar implements ImportBeanDefinitionRegistrar, EnvironmentAware {

    private Environment environment;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

        ConfigurationProperties annotation = RedisMultiSourcesProperties.class.getAnnotation(ConfigurationProperties.class);
        BindResult<RedisMultiSourcesProperties> bind = Binder.get(environment).bind(annotation.prefix(), RedisMultiSourcesProperties.class);
        RedisMultiSourcesProperties redisMultiSourcesProperties = bind.get();

        if (Objects.isNull(redisMultiSourcesProperties) || CollectionUtils.isEmpty(redisMultiSourcesProperties.getSources())) {
            return;
        }

        if (registry instanceof ConfigurableListableBeanFactory beanFactory) {

            // Whether it is virtual threads.
            boolean active = Threading.VIRTUAL.isActive(environment);

            redisMultiSourcesProperties.getSources().forEach((name, source) -> {

                // Whether it is Primary
                boolean isPrimary = name.equals(redisMultiSourcesProperties.getPrimaryKey());

                // PropertiesRedisMultiSourcesConnectionDetails registration
                BeanDefinition connectionDetailsBD = BeanDefinitionBuilder.genericBeanDefinition(PropertiesRedisMultiSourcesConnectionDetails.class,
                        () -> new PropertiesRedisMultiSourcesConnectionDetails(source)).getBeanDefinition();
                connectionDetailsBD.setPrimary(isPrimary);
                String connectionDetailsBDN = name + PropertiesRedisMultiSourcesConnectionDetails.class.getSimpleName();
                registry.registerBeanDefinition(connectionDetailsBDN, connectionDetailsBD);


                // LettuceConnectionFactory registration
                BeanDefinition connectionFactoryBD = BeanDefinitionBuilder.genericBeanDefinition(LettuceConnectionFactory.class, () -> {

                    // new LettuceMultiSourcesConnectionConfiguration
                    PropertiesRedisMultiSourcesConnectionDetails connectionDetails = beanFactory.getBean(connectionDetailsBDN, PropertiesRedisMultiSourcesConnectionDetails.class);
                    ObjectProvider<RedisStandaloneConfiguration> standaloneProvider = beanFactory.getBeanProvider(RedisStandaloneConfiguration.class);
                    ObjectProvider<RedisSentinelConfiguration> sentinelProvider = beanFactory.getBeanProvider(RedisSentinelConfiguration.class);
                    ObjectProvider<RedisClusterConfiguration> clusterProvider = beanFactory.getBeanProvider(RedisClusterConfiguration.class);
                    ObjectProvider<SslBundles> sslBundlesProvider = beanFactory.getBeanProvider(SslBundles.class);

                    LettuceMultiSourcesConnectionConfiguration lettuceMultiSourcesConnectionConfiguration = new LettuceMultiSourcesConnectionConfiguration(source,
                            standaloneProvider,
                            sentinelProvider,
                            clusterProvider,
                            connectionDetails,
                            sslBundlesProvider);

                    ObjectProvider<LettuceClientConfigurationBuilderCustomizer> configurationBuilderCustomizerProvider = beanFactory.getBeanProvider(LettuceClientConfigurationBuilderCustomizer.class);
                    ObjectProvider<LettuceClientOptionsBuilderCustomizer> optionsBuilderCustomizerProvider = beanFactory.getBeanProvider(LettuceClientOptionsBuilderCustomizer.class);
                    ClientResources clientResources = beanFactory.getBean(ClientResources.class);

                    LettuceConnectionFactory factory = lettuceMultiSourcesConnectionConfiguration.createConnectionFactory(configurationBuilderCustomizerProvider, optionsBuilderCustomizerProvider, clientResources);


                    if (active) {
                        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("redis-");
                        executor.setVirtualThreads(true);
                        factory.setExecutor(executor);
                    }
                    return factory;
                }).getBeanDefinition();

                connectionFactoryBD.setPrimary(isPrimary);
                String connectionFactoryBDN = name + LettuceConnectionFactory.class.getSimpleName();
                registry.registerBeanDefinition(connectionFactoryBDN, connectionFactoryBD);
            });

        }
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }


}
