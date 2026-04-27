package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private ShopTypeMapper shopTypeMapper;

    @Override
    public List<ShopType> qureylist() {
      List<String> shopType = stringRedisTemplate.opsForList().range("cache:shop:type", 0, -1);
        if(shopType!=null&&shopType.size()>0){
            List<ShopType> shopTypes = new ArrayList<>();
            for (String s : shopType) {
                ShopType shopType1 = JSONUtil.toBean(s, ShopType.class);
                shopTypes.add(shopType1);
            }
            return shopTypes;
        }
        List<ShopType> typeList = query().orderByAsc("sort").list();

        if(typeList==null){
            return Collections.emptyList();
        }
        for (ShopType shopTyp : typeList) {
            stringRedisTemplate.opsForList().rightPush("cache:shop:type",JSONUtil.toJsonStr(shopTyp));
        }
        return typeList;
    }
}
