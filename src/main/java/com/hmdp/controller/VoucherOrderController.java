package com.hmdp.controller;


import com.hmdp.dto.Result;
import com.hmdp.service.IVoucherOrderService;
import com.hmdp.service.impl.RedisLimitService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;



/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/voucher-order")
public class VoucherOrderController {

    @Resource
    private IVoucherOrderService voucherOrderService;

    @Resource
    private RedisLimitService redisLimitService;

    @PostMapping("seckill/{id}")
    public Result seckillVoucher(@PathVariable("id") Long voucherId) {

        // 令牌桶限流：桶容量5000，每秒补充5000令牌（平滑限流）
        boolean allowed = redisLimitService.tryAcquireWithTokenBucket(
                "seckill:global",
                5000,
                5000.0
        );

        if (!allowed) {
            return Result.fail("当前活动太火爆，请稍后重试");
        }

        return voucherOrderService.seckillVoucher(voucherId);
    }
}