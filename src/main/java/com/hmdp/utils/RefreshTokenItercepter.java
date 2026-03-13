package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;


public class RefreshTokenItercepter implements HandlerInterceptor {


    private StringRedisTemplate stringRedisTemplate;
    public RefreshTokenItercepter(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //从请求头获取token
        String token = request.getHeader("authorization");
        //用token获取redis中的用户
        if (StrUtil.isBlankIfStr(token)){
            return true;
        }
        Map<Object, Object> user= stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY+token);
        //判断用户是否存在
        if (user.isEmpty()){
            //不存在拦截器拦截
            return true;
        }
        //将查询到的信息放入UserDTO
        UserDTO userDTO = BeanUtil.fillBeanWithMap(user, new UserDTO(), false);
        //存在，用户信息放入ThreadLocal
        UserHolder.saveUser((UserDTO) userDTO);
        //TODO刷新有效期
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //移除用户
        UserHolder.removeUser();
    }
}
