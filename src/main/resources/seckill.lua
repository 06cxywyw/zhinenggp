-- 参数：优惠券ID， 用户ID
local voucherId = ARGV[1]
local userId = ARGV[2]

-- 拼接Key
local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

-- 1. 判断库存是否充足
local stock = redis.call('get', stockKey)
if (not stock) or tonumber(stock) <= 0 then
    return 1
end

-- 2. 判断用户是否已经下单
if (redis.call('sismember', orderKey, userId) == 1) then
    return 2
end

-- 3. 扣减库存
redis.call('decr', stockKey)
-- 4. 将用户ID加入已购买集合
redis.call('sadd', orderKey, userId)

-- 成功
return 0