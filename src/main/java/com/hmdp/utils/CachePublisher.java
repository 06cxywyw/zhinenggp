package com.hmdp.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

/**
 * 缓存消息发布器：发布缓存清除消息到 Redis Pub/Sub
 */
@Component
@Slf4j
public class CachePublisher {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发布缓存清除消息
     *
     * @param cacheName 缓存名称
     * @param id        缓存ID，传入"all"表示清除所有缓存
     */
    public void publishCacheClear(String cacheName, String id) {
        String channel = RedisConstants.CACHE_CLEAR_CHANNEL;
        String message = cacheName + ":" + id;

        stringRedisTemplate.convertAndSend(channel, message);
        log.info("发布缓存清除消息: channel={}, message={}", channel, message);
    }
}
