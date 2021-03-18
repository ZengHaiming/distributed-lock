package com.zenghm.distributed.lock.core;

/**
 * @author Airlen
 * @date 2021/3/12
 * @description 锁的上下文
 */
public interface LockContext {
    String getNamespace();
    String getKey();
    String getValue();
    long getTimeout();
    long getThreadId();
    LockState getLockState();
}
