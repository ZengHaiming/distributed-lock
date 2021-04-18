package com.zenghm.distributed.lock.core;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
public interface LockConstant {
    /**
     * 自旋时间，毫秒
     */
    long SPIN_TIME = 10L;
    /**
     * 分布式锁的key的根路径
     */
    String ROOT_PATH = "DistributedLock";
}
