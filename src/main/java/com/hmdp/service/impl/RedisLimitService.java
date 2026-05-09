package com.hmdp.service.impl;


import jakarta.annotation.Resource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;


import java.util.Collections;

@Service
public class RedisLimitService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final DefaultRedisScript<Long> LIMIT_SCRIPT;
    private static final DefaultRedisScript<Long> TOKEN_BUCKET_SCRIPT;

    static {

        LIMIT_SCRIPT = new DefaultRedisScript<>();

        LIMIT_SCRIPT.setLocation(
                new ClassPathResource("limit.lua")
        );

        LIMIT_SCRIPT.setResultType(Long.class);


        TOKEN_BUCKET_SCRIPT = new DefaultRedisScript<>();

        TOKEN_BUCKET_SCRIPT.setLocation(
                new ClassPathResource("token_bucket.lua")
        );

        TOKEN_BUCKET_SCRIPT.setResultType(Long.class);
    }

    /**
     * 固定窗口限流
     */
    public boolean tryAcquire(String key, int limit, int seconds) {
        Long result = stringRedisTemplate.execute(
                LIMIT_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(limit),
                String.valueOf(seconds)
        );
        return result != null && result == 1;
    }

    /**
     * 令牌桶限流
     *
     * @param key          限流键
     * @param capacity     桶容量（最大令牌数）
     * @param refillRate   令牌补充速率（令牌/秒）
     * @return true表示允许请求，false表示被限流
     */
    public boolean tryAcquireWithTokenBucket(String key, int capacity, double refillRate) {
        long now = System.currentTimeMillis();
        Long result = stringRedisTemplate.execute(
                TOKEN_BUCKET_SCRIPT,
                Collections.singletonList(key),
                String.valueOf(capacity),
                String.valueOf(refillRate),
                String.valueOf(now)
        );
        return result != null && result == 1;
    }
}
