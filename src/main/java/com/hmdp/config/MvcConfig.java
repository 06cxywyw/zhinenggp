package com.hmdp.config;

import com.hmdp.utils.LoginIntercepter;
import com.hmdp.utils.RefreshTokenItercepter;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

   @Override
   public void addInterceptors(InterceptorRegistry registry) {
       registry.addInterceptor(new LoginIntercepter())
               .excludePathPatterns(
                       "/user/code",
                       "/user/login",
                       "/blog/hot",
                       "/upload/**",
                       "/shop-type/**",
                       "/voucher/**",
                       "/shop/**"

               ).order(1);
       registry.addInterceptor(new RefreshTokenItercepter(stringRedisTemplate))
               .addPathPatterns("/**").order(0);
   }

}
