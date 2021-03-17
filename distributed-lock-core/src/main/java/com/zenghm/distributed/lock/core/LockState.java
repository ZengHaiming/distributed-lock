package com.zenghm.distributed.lock.core;

/**
 * @author Airlen
 * @date 2021/3/12
 * @description 标识锁的状态
 */
public enum  LockState {
    /**
     * 等待
     */
    WAIT,
    /**
     * 锁定
     */
    LOCKING,

    /**
     * 释放
     */
    RELEASE
}
