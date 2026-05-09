package com.hmdp.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hmdp.entity.Shop;
import com.hmdp.utils.CacheListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class CaffeineConfig {

    @Bean
    public Cache<Long, Shop> shopCache(){
        return Caffeine.newBuilder()
                .initialCapacity(100)
                .maximumSize(10000)
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();
    }

    /**
     * Redis 消息监听器容器
     */
    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter messageListenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // 订阅缓存清除频道
        container.addMessageListener(messageListenerAdapter, new PatternTopic("cache:clear"));
        log.info("Redis 消息监听器容器已启动");
        return container;
    }

    /**
     * 消息监听器适配器，绑定监听器
     */
    @Bean
    public MessageListenerAdapter messageListenerAdapter(CacheListener cacheListener) {
        return new MessageListenerAdapter(cacheListener);
    }
}
