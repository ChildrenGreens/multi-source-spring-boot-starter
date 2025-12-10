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
 * Centralized Redis class names used for reflection/runtime hints.
 */
final class RedisDataClassNames {

    static final String PROPERTIES_DATA_REDIS_CONNECTION_DETAILS = "org.springframework.boot.data.redis.autoconfigure.PropertiesDataRedisConnectionDetails";

    static final String LETTUCE_CONNECTION_CONFIGURATION = "org.springframework.boot.data.redis.autoconfigure.LettuceConnectionConfiguration";

    static final String JEDIS_CONNECTION_CONFIGURATION = "org.springframework.boot.data.redis.autoconfigure.JedisConnectionConfiguration";

    static final String JEDIS_TYPE = "redis.clients.jedis.Jedis";

    private RedisDataClassNames() {
    }
}
