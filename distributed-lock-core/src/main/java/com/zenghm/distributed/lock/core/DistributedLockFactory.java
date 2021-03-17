package com.zenghm.distributed.lock.core;

import com.zenghm.distributed.lock.core.exception.DistributedLockException;
import com.zenghm.distributed.lock.core.exception.DistributedLockLoseException;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
@Service
public class DistributedLockFactory implements DistributedLock, ApplicationContextAware {
    /**
     * 容器上下文
     */
    private ApplicationContext applicationContext;
    /**
     * 需要注意多线程安全问题
     */
    private ThreadLocal<DistributedLock> distributedLockThreadLocal = new ThreadLocal<>();

    /**
     * 是否能进行处理
     *
     * @return
     */
    @Override
    public boolean handler(LockContext lockContext) {
        return false;
    }

    @Override
    public void setLockContext(LockContext context) {
        Map<String, DistributedLock> distributedLockMap = getDistributedLocks();
        for (DistributedLock distributedLock : distributedLockMap.values()) {
            if (distributedLock.handler(context)) {
                distributedLock.setLockContext(context);
                distributedLockThreadLocal.set(distributedLock);
            }
        }
    }

    /**
     * 获取锁的状态
     *
     * @return
     */
    @Override
    public LockState getLockState() throws DistributedLockException {
        contextExceptionNotSet();
        return this.distributedLockThreadLocal.get().getLockState();
    }

    /**
     * 获取锁的当前持有线程id
     */
    @Override
    public long getCurrentHoldThread() throws DistributedLockException{
        contextExceptionNotSet();
        return this.distributedLockThreadLocal.get().getCurrentHoldThread();
    }

    /**
     * Acquires the lock.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until the
     * lock has been acquired.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation may be able to detect erroneous use
     * of the lock, such as an invocation that would cause deadlock, and
     * may throw an (unchecked) exception in such circumstances.  The
     * circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     */
    @Override
    public void lock() throws DistributedLockException {
        contextExceptionNotSet();
        this.distributedLockThreadLocal.get().lock();
    }

    /**
     * Acquires the lock unless the current thread is
     * {@linkplain Thread#interrupt interrupted}.
     *
     * <p>Acquires the lock if it is available and returns immediately.
     *
     * <p>If the lock is not available then the current thread becomes
     * disabled for thread scheduling purposes and lies dormant until
     * one of two things happens:
     *
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported.
     * </ul>
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring the
     * lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some
     * implementations may not be possible, and if possible may be an
     * expensive operation.  The programmer should be aware that this
     * may be the case. An implementation should document when this is
     * the case.
     *
     * <p>An implementation can favor responding to an interrupt over
     * normal method return.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would
     * cause deadlock, and may throw an (unchecked) exception in such
     * circumstances.  The circumstances and the exception type must
     * be documented by that {@code Lock} implementation.
     *
     * @throws InterruptedException if the current thread is
     *                              interrupted while acquiring the lock (and interruption
     *                              of lock acquisition is supported)
     */
    @Override
    public void lockInterruptibly() throws InterruptedException, DistributedLockException {
        contextExceptionNotSet();
        this.distributedLockThreadLocal.get().lockInterruptibly();
    }

    /**
     * Acquires the lock only if it is free at the time of invocation.
     *
     * <p>Acquires the lock if it is available and returns immediately
     * with the value {@code true}.
     * If the lock is not available then this method will return
     * immediately with the value {@code false}.
     *
     * <p>A typical usage idiom for this method would be:
     * <pre> {@code
     * Lock lock = ...;
     * if (lock.tryLock()) {
     *   try {
     *     // manipulate protected state
     *   } finally {
     *     lock.unlock();
     *   }
     * } else {
     *   // perform alternative actions
     * }}</pre>
     * <p>
     * This usage ensures that the lock is unlocked if it was acquired, and
     * doesn't try to unlock if the lock was not acquired.
     *
     * @return {@code true} if the lock was acquired and
     * {@code false} otherwise
     */
    @Override
    public boolean tryLock() throws DistributedLockException {
        contextExceptionNotSet();
        return this.distributedLockThreadLocal.get().tryLock();
    }

    /**
     * Acquires the lock if it is free within the given waiting time and the
     * current thread has not been {@linkplain Thread#interrupt interrupted}.
     *
     * <p>If the lock is available this method returns immediately
     * with the value {@code true}.
     * If the lock is not available then
     * the current thread becomes disabled for thread scheduling
     * purposes and lies dormant until one of three things happens:
     * <ul>
     * <li>The lock is acquired by the current thread; or
     * <li>Some other thread {@linkplain Thread#interrupt interrupts} the
     * current thread, and interruption of lock acquisition is supported; or
     * <li>The specified waiting time elapses
     * </ul>
     *
     * <p>If the lock is acquired then the value {@code true} is returned.
     *
     * <p>If the current thread:
     * <ul>
     * <li>has its interrupted status set on entry to this method; or
     * <li>is {@linkplain Thread#interrupt interrupted} while acquiring
     * the lock, and interruption of lock acquisition is supported,
     * </ul>
     * then {@link InterruptedException} is thrown and the current thread's
     * interrupted status is cleared.
     *
     * <p>If the specified waiting time elapses then the value {@code false}
     * is returned.
     * If the time is
     * less than or equal to zero, the method will not wait at all.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>The ability to interrupt a lock acquisition in some implementations
     * may not be possible, and if possible may
     * be an expensive operation.
     * The programmer should be aware that this may be the case. An
     * implementation should document when this is the case.
     *
     * <p>An implementation can favor responding to an interrupt over normal
     * method return, or reporting a timeout.
     *
     * <p>A {@code Lock} implementation may be able to detect
     * erroneous use of the lock, such as an invocation that would cause
     * deadlock, and may throw an (unchecked) exception in such circumstances.
     * The circumstances and the exception type must be documented by that
     * {@code Lock} implementation.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return {@code true} if the lock was acquired and {@code false}
     * if the waiting time elapsed before the lock was acquired
     * @throws InterruptedException if the current thread is interrupted
     *                              while acquiring the lock (and interruption of lock
     *                              acquisition is supported)
     */
    @Override
    public boolean tryLock(long time, TimeUnit unit) throws DistributedLockException {
        contextExceptionNotSet();
        return this.distributedLockThreadLocal.get().tryLock(time, unit);
    }

    /**
     * Releases the lock.
     *
     * <p><b>Implementation Considerations</b>
     *
     * <p>A {@code Lock} implementation will usually impose
     * restrictions on which thread can release a lock (typically only the
     * holder of the lock can release it) and may throw
     * an (unchecked) exception if the restriction is violated.
     * Any restrictions and the exception
     * type must be documented by that {@code Lock} implementation.
     */
    @Override
    public void unlock() throws DistributedLockException {
        contextExceptionNotSet();
        this.distributedLockThreadLocal.get().unlock();
        /**
         * 同时移除
         */
        this.distributedLockThreadLocal.remove();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private Map<String, DistributedLock> getDistributedLocks() {
        return this.applicationContext.getBeansOfType(DistributedLock.class);
    }

    private void contextExceptionNotSet() throws DistributedLockException {
        if(distributedLockThreadLocal.get()==null){
            throw new DistributedLockException("Context exception not set.");
        }
    }

}
