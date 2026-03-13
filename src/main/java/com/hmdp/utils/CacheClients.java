package com.hmdp.utils;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.sun.xml.internal.bind.v2.model.core.ID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;


@Component
@Slf4j
public class CacheClients {

    @Resource
    private  StringRedisTemplate stringRedisTemplate;

    public void set(String key, Object value, Long time, TimeUnit unit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value), time, unit);
    }

    public void setLogicalExpire(String key, Object value, Long time, TimeUnit unit){
        //封装逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(unit.toSeconds( time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData), time, unit);
    }

    public <R,ID> R queryWithPassThrough(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback, Long
            time, TimeUnit unit){
        //从redis查缓存
        String key = keyPrefix+id;
        String Json = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if(StrUtil.isNotBlank(Json)) {
            return JSONUtil.toBean(Json, type);
            //存在返回
        }
        //判断命中的是否是空值,命中为null上面已处理,下面处理值为""
        if(Json!=null){
            return null;
        }
        //不存在，查数据库
        R r = dbFallback.apply(id);
        //不存在，将""存入redis避免缓存穿透
        if(r==null){
            this.set(key,"",time,unit);
            return null;
        }
        //存在，写入缓存
        this.set(key,r,time,unit);
        //返回
        return r;
    }


    private static final ExecutorService excutorService = Executors.newFixedThreadPool(10);

    public <R,ID> R queryWithLogicalExpire(String keyPrefix, ID id, Class<R> type, Function<ID, R> dbFallback,
                                           Long time, TimeUnit unit) {
        String key = keyPrefix+id;
        //从redis查缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            //为空，返回null
            return null;
        }
        //命中，需要先把json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        R r = JSONUtil.toBean(data, type);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            //1.未过期
            return r;
        }
        //2.已过期，缓存重建
        String lockKey = LOCK_SHOP_KEY + id;
        //尝试获取互斥锁
        boolean islock = tryLock(lockKey);
        //是否获取锁成功
        if (islock) {
            //TODO获取锁成功，开启独立 线程·实现缓存重建
            excutorService.submit(() -> {
                try {
                    //查询店铺数据
                    R r1 = dbFallback.apply(id);
                    //写入redis
                    this.setLogicalExpire(key, r1,time,unit);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    //释放锁
                    unLock(lockKey);
                }
            });
        }
        //返回
        return r;
    }
    private boolean tryLock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void unLock(String key){
        stringRedisTemplate.delete(key);
    }

}
