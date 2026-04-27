package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.benmanes.caffeine.cache.Cache;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClients;
import com.hmdp.utils.RedisData;
import com.hmdp.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;



import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {



    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private CacheClients cacheClients;
    @Autowired
    private Cache<Long, Shop> shopCache;


/*
    jemeter测压代码
    // 0 = DB直查，1 = Redis，2 = Redis + Caffeine
    private int mode =2;
    // 模式0：纯数据库
        if (mode == 0) {
        log.info("查询数据库 id={}", id);
        try {
            Thread.sleep(20); // 👈 模拟数据库耗时（20ms）
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Shop shop = getById(id);
        return Result.ok(shop);
    }

    // 模式2：先查本地缓存（Caffeine）
        if (mode == 2) {
        Shop shop = shopCache.getIfPresent(id);
        if (shop != null) {
            return Result.ok(shop);
        }
    }

    // Redis 查询（模式1 和 模式2都会走）
    Shop shop = cacheClients.queryWithPassThrough(
            CACHE_SHOP_KEY,
            id,
            Shop.class,
            this::getById,
            CACHE_SHOP_TTL,
            TimeUnit.MINUTES
    );

        if (shop == null) {
        return Result.fail("店铺不存在!");
    }

    // 模式2：写入本地缓存
        if (mode == 2) {
        shopCache.put(id, shop);
    }

        return Result.ok(shop);
}*/
    @Override
    public Result queryById(Long id) {
        Shop shop=shopCache.getIfPresent(id);
        if(shop!=null){
            return Result.ok(shop);
        }


        //缓存穿透
        shop = cacheClients.queryWithPassThrough(CACHE_SHOP_KEY, id,Shop.class,id2 ->getById(id2),
                CACHE_SHOP_TTL, TimeUnit.MINUTES);
        /*Shop shop = queryWithMutex(id);*////互斥锁解决缓存击穿
        //逻辑过期解决缓存击穿
       /*Shop shop = cacheClients.queryWithLogicalExpire(CACHE_SHOP_KEY, id,Shop.class,id2 ->getById(id2),
               2L, TimeUnit.SECONDS);*/
        if(shop==null){
            return Result.fail("店铺不存在!");
        }
        shopCache.put(id,shop);
        return Result.ok(shop);
    }
    private Shop queryWithMutex(Long id){

        //从redis查缓存
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        //判断是否存在
        if(StrUtil.isNotBlank(shopJson)) {
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            //存在返回
            return shop;
        }
        //判断命中的是否是空值,命中为null上面已处理,下面处理值为""
        if(shopJson!=null){
            return null;
        }

        //实现缓存重建
        //4.1获取互斥锁
        String lockKey=LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean islock = tryLock(lockKey);
            //4.2判断是否获取成功
            if(!islock){
                //4.3失败，休眠并重试
                Thread.sleep(50);
                return  queryWithMutex(id);
            }
            //4.4成功，查数据库
            shop = getById(id);
            //模拟重建延迟
            Thread.sleep(200);
            //不存在，将""存入redis避免缓存穿透
            if(shop==null){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",2, TimeUnit.MINUTES);
                return null;
            }
            //存在，写入缓存
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop), 30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        finally {
            //释放互斥锁
            unLock(lockKey);
        }
        //返回
        return shop;
    }

        //分布式锁
        private boolean tryLock(String key){
            Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
            return BooleanUtil.isTrue(flag);
        }
        private void unLock(String key){
            stringRedisTemplate.delete(key);
        }


    @Override
    public Result saveshop(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        save(shop);
        shopCache.invalidate( id);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }

    @Transactional
    @Override
    public Result updateshop(Shop shop) {
        Long id = shop.getId();
        if(id==null){
            return Result.fail("店铺id不能为空");
        }
        updateById( shop);
        shopCache.invalidate(id);
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());
        return Result.ok();
    }

    public void saveShop2Redis(long l, long l1) {
        Shop shop = getById(l);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(l1));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+l,JSONUtil.toJsonStr(redisData));
    }

    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.判断是否需要根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，按数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            // 返回数据
            return Result.ok(page.getRecords());
        }

        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE;
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE;

        // 3.查询redis、按照距离排序、分页。结果：shopId、distance
        String key = SHOP_GEO_KEY + typeId;
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo() // GEOSEARCH key BYLONLAT x y BYRADIUS 10 WITHDISTANCE
                .search(
                        key,
                        GeoReference.fromCoordinate(x, y),
                        new Distance(5000),
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().limit(end)
                );
        // 4.解析出id
        if (results == null) {
            return Result.ok(Collections.emptyList());
        }
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            // 没有下一页了，结束
            return Result.ok(Collections.emptyList());
        }
        // 4.1.截取 from ~ end的部分
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        list.stream().skip(from).forEach(result -> {
            // 4.2.获取店铺id
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            // 4.3.获取距离
            Distance distance = result.getDistance();
            distanceMap.put(shopIdStr, distance);
        });
        // 5.根据id查询Shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query().in("id", ids).last("ORDER BY FIELD(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }
}
