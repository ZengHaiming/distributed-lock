package com.zenghm.distributed.lock.core;

/**
 * @author Airlen
 * @date 2021/3/19
 * @description xxx
 */
public interface LockCallback<R> {
    /**
     * 分布式锁回调函数 , 获取锁成功后调用
     * @param context 锁上下文
     * @return
     */
    R callback(LockContext context);
}
