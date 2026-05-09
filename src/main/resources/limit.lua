local key = KEYS[1]

local limit = tonumber(ARGV[1])

local expireTime = tonumber(ARGV[2])

local current = redis.call("get", key)

-- 超过限制
if current and tonumber(current) >= limit then
    return 0
end

-- 请求次数+1
current = redis.call("incr", key)

-- 第一次设置过期时间
if tonumber(current) == 1 then
    redis.call("expire", key, expireTime)
end

return 1