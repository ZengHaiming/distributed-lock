package com.zenghm.distributed.lock.core;

/**
 * @author Airlen
 * @date 2021/3/19
 * @description xxx
 */
public interface DefaultDistributedLock {
    <T> T lock(LockContext context,LockCallback<T> callback);
}
