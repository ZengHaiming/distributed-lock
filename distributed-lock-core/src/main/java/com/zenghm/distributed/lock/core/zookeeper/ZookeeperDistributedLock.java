package com.zenghm.distributed.lock.core.zookeeper;

import com.zenghm.distributed.lock.core.DistributedLock;
import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
@Service
public class ZookeeperDistributedLock implements DistributedLock {
    /**
     * 是否能进行处理
     *
     * @param lockContext
     * @return
     */
    @Override
    public boolean handler(LockContext lockContext) {
        return lockContext instanceof ZookeeperLockContext;
    }

    @Override
    public void lock(String namespace) {

    }

    @Override
    public void lockInterruptibly(String namespace) throws InterruptedException {

    }


    @Override
    public boolean tryLock(String namespace) {
        return false;
    }


    @Override
    public boolean tryLock(String namespace,long time, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public void unlock(String namespace) {

    }

    @Override
    public void setLockContext(LockContext context) {

    }

    /**
     * 获取锁的状态
     *
     * @return
     */
    @Override
    public LockState getLockState(String namespace) {
        return null;
    }

    /**
     * 获取锁的当前持有线程id
     */
    @Override
    public long getCurrentHoldThread(String namespace) {
        return 0;
    }
}
