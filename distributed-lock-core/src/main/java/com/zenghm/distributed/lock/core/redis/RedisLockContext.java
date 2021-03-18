package com.zenghm.distributed.lock.core.redis;

import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;

import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
public class RedisLockContext implements LockContext {
    private String namespace;
    private String key;
    private String value;
    private long timeout;
    private long threadId;
    private LockState state;
    private ScheduledThreadPoolExecutor scheduler;

    public RedisLockContext(String namespace,String key, long timeout) {
        this.namespace = namespace;
        this.key = key;
        this.value = UUID.randomUUID().toString();
        this.timeout = timeout;
        this.threadId = Thread.currentThread().getId();
        this.state = LockState.WAIT;
    }

    public ScheduledThreadPoolExecutor getScheduler() {
        return scheduler;
    }

    public void setScheduler(ScheduledThreadPoolExecutor scheduler) {
        this.scheduler = scheduler;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public void setState(LockState state) {
        this.state = state;
    }

    @Override
    public String getNamespace() {
        return this.namespace;
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
        return this.threadId;
    }

    @Override
    public LockState getLockState() {
        return this.state;
    }
}
