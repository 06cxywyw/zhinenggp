package com.hmdp.utils;

import com.hmdp.entity.VoucherOrder;
import com.hmdp.service.impl.VoucherOrderServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SeckillOrderConsumer {

    @Resource
    private VoucherOrderServiceImpl voucherOrderService;

    @Resource
    private RedissonClient redissonClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "direct.seckill.queue"),
            exchange = @Exchange(name = "hmdianping.direct", type = ExchangeTypes.DIRECT),
            key = "direct.seckill"
    ))
    public void listenOrderCreate(VoucherOrder voucherOrder) {
        log.info("接收到订单创建消息: {}", voucherOrder.getId());
        try {
            handleVoucherOrder(voucherOrder);
        } catch (Exception e) {
            log.error("处理订单异常, orderId: {}", voucherOrder.getId(), e);
            // 可加入重试逻辑：死信队列或重新放回队列
        }
    }

    private void handleVoucherOrder(VoucherOrder voucherOrder) throws InterruptedException {
        Long userId = voucherOrder.getUserId();
        String lockKey = "lock:order:" + userId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean isLock = false;
        try {
            // 获取用户锁，等待5秒，自动释放10秒
            isLock = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!isLock) {
                log.error("用户{}的订单正在处理，重复请求被丢弃，orderId: {}", userId, voucherOrder.getId());
                return;
            }

            // 执行落库逻辑
            voucherOrderService.createVoucherOrderInDB(voucherOrder);
        } finally {
            if (isLock) {
                lock.unlock();
            }
        }
    }
}