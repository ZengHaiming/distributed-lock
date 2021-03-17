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
        RedisLockContext redisLockContext = (RedisLockContext) context;
        redisLockContext.setState(LockState.WAIT);
        redisLockContext.setThreadId(Thread.currentThread().getId());
        redisLockContext.setScheduler(new ScheduledThreadPoolExecutor(1));
        contextThreadLocal.set(redisLockContext);
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
         * 1、需要判断当前线程是否已经获取到锁 ，未做
         * 2、尝试获取锁
         */
        if(LockState.LOCKING.equals(contextThreadLocal.get().getLockState())){
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
            contextThreadLocal.get().getScheduler().scheduleWithFixedDelay(this::monitorTask,
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
         * 保证持有人解锁 , 此处需要修改为保证原子性的操作
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
     * 监测锁的状态，给锁进行续时
     */
    private Boolean monitorTask() {
        /**
         * 此处需要修改为保持原子性的操作， 存在锁丢失的可能 ，例如：redis 主从 、哨兵模式
         */
        if (stringRedisTemplate.hasKey(contextThreadLocal.get().getKey())) {
            Date now = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.SECOND, (int) contextThreadLocal.get().getTimeout());
            return stringRedisTemplate.expireAt(contextThreadLocal.get().getKey(), calendar.getTime());
        }
        return false;
    }
}
