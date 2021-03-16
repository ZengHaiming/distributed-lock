package com.zenghm.distributed.lock.core.exception;

/**
 * @author Airlen
 * @date 2021/3/12
 * @description 分布式锁丢失异常
 */
public class DistributedLockLoseException extends DistributedLockException{
    /**
     * Constructs a new exception with the specified detail message.  The
     * cause is not initialized, and may subsequently be initialized by
     * a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for
     *                later retrieval by the {@link #getMessage()} method.
     */
    public DistributedLockLoseException(String message) {
        super(message);
    }
}
