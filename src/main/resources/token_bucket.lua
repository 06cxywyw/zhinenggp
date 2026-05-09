local key = KEYS[1]
local capacity = tonumber(ARGV[1])      -- 桶容量
local refillRate = tonumber(ARGV[2])   -- 补充速率（令牌/秒）
local now = tonumber(ARGV[3])          -- 当前时间（毫秒）

-- 获取桶状态
local bucket = redis.call("HMGET", key, "tokens", "last_time")
local tokens = tonumber(bucket[1]) or capacity
local lastTime = tonumber(bucket[2]) or now

-- 计算距离上次补充的时间差（秒）
local deltaTime = (now - lastTime) / 1000

-- 补充令牌：不能超过容量
tokens = math.min(capacity, tokens + deltaTime * refillRate)

-- 尝试消费令牌
if tokens >= 1 then
    tokens = tokens - 1
    redis.call("HMSET", key, "tokens", tokens, "last_time", now)
    redis.call("EXPIRE", key, math.ceil(capacity / refillRate) + 1)
    return 1  -- 允许请求
else
    return 0  -- 拒绝请求
end
