package com.zenghm.distributed.lock.core.zookeeper;

import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
public class ZookeeperLockContext implements LockContext {
    @Override
    public String getNamespace() {
        return null;
    }

    @Override
    public String getKey() {
        return null;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public long getTimeout() {
        return 0;
    }

    @Override
    public long getThreadId() {
        return 0;
    }

    @Override
    public LockState getLockState() {
        return null;
    }
}
