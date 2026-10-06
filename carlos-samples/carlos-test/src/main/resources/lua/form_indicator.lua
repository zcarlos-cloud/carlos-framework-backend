-- KEYS[1] : form:stat:{formId}
-- ARGV[1] : 行数增量（可负）
-- ARGV[2] : 本次时间戳
-- ARGV[3] : pv 增量（可 0）
local cnt = redis.call('HINCRBY', KEYS[1], 'cnt', ARGV[1])
redis.call('HSET', KEYS[1], 'ts', ARGV[2])
if ARGV[3] ~= '0' then
    redis.call('HINCRBY', KEYS[1], 'pv', ARGV[3])
end
-- 返回整个Hash
local result = redis.call('HGETALL', KEYS[1])
return result
-- return { cnt, redis.call('HGET', KEYS[1], 'pv'), redis.call('HGET', KEYS[1], 'ts') }
