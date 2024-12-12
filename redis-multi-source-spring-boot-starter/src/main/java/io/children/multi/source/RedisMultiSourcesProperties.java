package io.children.multi.source;

import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

/**
 * Configuration properties for multi data source Redis.
 *
 * @author ChildrenGreens
 */
@ConfigurationProperties(prefix = "spring.multi-sources.redis")
public class RedisMultiSourcesProperties {

    private Map<String, RedisProperties> sources;

    private String primaryKey;

    public Map<String, RedisProperties> getSources() {
        return sources;
    }

    public void setSources(Map<String, RedisProperties> sources) {
        this.sources = sources;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }
}
