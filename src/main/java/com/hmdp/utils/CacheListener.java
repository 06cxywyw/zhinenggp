package com.hmdp.utils;

import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.entity.Shop;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 缓存监听器：订阅 Redis Pub/Sub 消息，清除本地 Caffeine 缓存
 */
@Component
@Slf4j
public class CacheListener implements MessageListener {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private Cache<Long, Shop> shopCache;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        // 获取发布的频道和消息内容
        String channel = new String(message.getChannel());
        String payload = new String(message.getBody());

        // 只处理缓存清除消息
        if (RedisConstants.CACHE_CLEAR_CHANNEL.equals(channel)) {
            executorService.submit(() -> {
                try {
                    log.info("收到缓存清除消息: {}", payload);
                    // 解析消息：格式为 "cacheName:id" 或 "cacheName:all"
                    String[] parts = payload.split(":");
                    if (parts.length >= 2) {
                        String cacheName = parts[0];
                        String id = parts[1];

                        // 清除对应缓存
                        if ("shop".equals(cacheName)) {
                            if ("all".equals(id)) {
                                // 清除所有店铺缓存
                                log.info("清除所有店铺缓存");
                                shopCache.invalidateAll();
                            } else {
                                // 清除指定店铺缓存
                                Long shopId = Long.valueOf(id);
                                log.info("清除店铺缓存: shopId={}", shopId);
                                shopCache.invalidate(shopId);
                            }
                        }
                    }
                } catch (Exception e) {
                    log.error("处理缓存清除消息失败", e);
                }
            });
        }
    }
}
