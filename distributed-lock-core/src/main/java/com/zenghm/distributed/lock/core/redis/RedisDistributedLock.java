package com.zenghm.distributed.lock.core.redis;

import com.zenghm.distributed.lock.core.DistributedLock;
import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description redis 实现分布式锁
 */
@Service
public class RedisDistributedLock implements DistributedLock {


    LockContext context;

    //@Autowired
    //Pool<Jedis> jedisPool;

    /**
     * 是否能进行处理
     *
     * @param lockContext
     * @return
     */
    @Override
    public boolean handler(LockContext lockContext) {
        return lockContext instanceof RedisLockContext;
    }

    @Override
    public void setLockContext(LockContext context) {
        this.context = context;
    }

    /**
     * 获取锁的状态
     *
     * @return
     */
    @Override
    public LockState getLockState() {
        return context.getLockState();
    }

    /**
     * 获取锁的当前持有线程id
     */
    @Override
    public long getCurrentHoldThread() {
        return context.getThreadId();
    }

    @Override
    public void lock() {

    }


    @Override
    public void lockInterruptibly() throws InterruptedException {

    }


    @Override
    public boolean tryLock() {
        return false;
    }


    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock() {

    }
}
