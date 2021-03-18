package com.zenghm.distributed.lock.core.redis;

import com.zenghm.distributed.lock.core.DistributedLock;
import com.zenghm.distributed.lock.core.LockConstant;
import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import com.zenghm.distributed.lock.core.exception.DistributedLockException;
import com.zenghm.distributed.lock.core.exception.DistributedLockLoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description redis 实现分布式锁
 */
@Service
public class RedisDistributedLock implements DistributedLock {
    /**
     * 需要注意多线程安全问题
     */
    private ThreadLocal<RedisLockContext> contextThreadLocal = new ThreadLocal<>();

    @Autowired
    StringRedisTemplate stringRedisTemplate;

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
        /**
         * 存在问题 TODO 一个线程只能获取到一个锁，不同的锁不能获取
         */
        if (contextThreadLocal.get() == null) {
            RedisLockContext redisLockContext = (RedisLockContext) context;
            redisLockContext.setState(LockState.WAIT);
            redisLockContext.setThreadId(Thread.currentThread().getId());
            redisLockContext.setScheduler(new ScheduledThreadPoolExecutor(1));
            contextThreadLocal.set(redisLockContext);
        }
    }

    /**
     * 获取锁的状态
     *
     * @return
     */
    @Override
    public LockState getLockState() {
        return contextThreadLocal.get().getLockState();
    }

    /**
     * 获取锁的当前持有线程id
     */
    @Override
    public long getCurrentHoldThread() {
        return contextThreadLocal.get().getThreadId();
    }

    @Override
    public void lock() throws DistributedLockException {
        if (!tryLock(contextThreadLocal.get().getTimeout() << 1, TimeUnit.SECONDS)) {
            throw new DistributedLockException("Distributed lock acquisition timeout.");
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {

    }


    @Override
    public boolean tryLock() {

        /**
         * 1、需要判断当前线程是否已经获取到锁 ，TODO 存在问题 ，需要判断是否是同一个锁
         * 2、尝试获取锁
         */
        if (LockState.LOCKING.equals(contextThreadLocal.get().getLockState())) {
            return true;
        }
        boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(contextThreadLocal.get().getKey(),
                contextThreadLocal.get().getValue(), contextThreadLocal.get().getTimeout(), TimeUnit.SECONDS);
        if (isLock) {
            /**
             * 获取到锁
             */
            contextThreadLocal.get().setState(LockState.LOCKING);
            /**
             * 开启锁监测
             */
            contextThreadLocal.get().getScheduler().scheduleWithFixedDelay(new Monitor(contextThreadLocal.get()),
                    contextThreadLocal.get().getTimeout() >> 1, contextThreadLocal.get().getTimeout() >> 1, TimeUnit.SECONDS);
        }
        return isLock;
    }


    @Override
    public boolean tryLock(long time, TimeUnit unit) throws DistributedLockException {
        /**
         * 如果未获取到锁则开始自旋
         */
        long sleep = LockConstant.SPIN_TIME;
        long timeout = TimeUnit.MILLISECONDS.convert(time, unit);
        while (!tryLock()) {
            try {
                /**
                 * 防止其他地方出现死锁
                 */
                if (sleep > timeout) {
                    return false;
                }
                Thread.sleep(sleep);
                sleep = sleep << 1;
            } catch (InterruptedException e) {
                throw new DistributedLockException("Distributed lock acquisition interrupt.");
            }
        }
        return true;
    }

    @Override
    public void unlock() throws DistributedLockLoseException {
        //先停止任务
        contextThreadLocal.get().getScheduler().shutdownNow();
        String value = stringRedisTemplate.opsForValue().get(contextThreadLocal.get().getKey());
        /**
         * 保证持有人解锁 , 此处需要修改为保证原子性的操作 TODO 需要修改为保持原子性
         */
        if (!StringUtils.hasLength(value) && value.equals(contextThreadLocal.get().getValue())
                && stringRedisTemplate.delete(contextThreadLocal.get().getKey())) {
            /**
             * 释放锁
             */
            contextThreadLocal.get().setState(LockState.RELEASE);
            contextThreadLocal.remove();
        } else {
            /**
             * 出现锁丢失
             */
            throw new DistributedLockLoseException("Distributed lock lost.");
        }
    }


    /**
     * 监测任务
     */
    class Monitor implements Runnable {
        private LockContext context;

        Monitor(LockContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            monitorTask(this.context);
        }

        /**
         * 监测锁的状态，给锁进行续时
         */
        private Boolean monitorTask(LockContext context) {
            /**
             * 此处需要修改为保持原子性的操作， 存在锁丢失的可能 ，例如：redis 主从 、哨兵模式 TODO 需要修改为保持原子性
             */
            if (stringRedisTemplate.hasKey(context.getKey())) {
                Date now = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(now);
                calendar.add(Calendar.SECOND, (int) context.getTimeout());
                return stringRedisTemplate.expireAt(context.getKey(), calendar.getTime());
            }
            return false;
        }
    }
}
