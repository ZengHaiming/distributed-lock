package com.zenghm.distributed.lock.core.redis;

import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
public class RedisLockContext implements LockContext {
    private String key;
    private String value;
    private long timeout;
    private long threadid;
    private LockState state;
    @Value("redis.redisConnectString")
    private String redisConnectString;
    public RedisLockContext(String key,String value,long timeout) {
        this.key = key;
        this.value = value;
        this.timeout = timeout;
        this.threadid = Thread.currentThread().getId();
        this.state = LockState.WAIT;
    }

    @Override
    public String getKey() {
        return this.key;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public long getTimeout() {
        return this.timeout;
    }

    @Override
    public long getThreadId() {
        return this.threadid;
    }

    @Override
    public LockState getLockState() {
        return this.state;
    }
}
