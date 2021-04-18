package com.zenghm.distributed.lock.core.redis;

import com.zenghm.distributed.lock.core.DistributedLock;
import com.zenghm.distributed.lock.core.LockConstant;
import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import com.zenghm.distributed.lock.core.exception.DistributedLockException;
import com.zenghm.distributed.lock.core.exception.DistributedLockLoseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description redis 实现分布式锁
 */
@Service
public class RedisDistributedLock implements DistributedLock {
    private final Logger logger = LoggerFactory.getLogger(RedisDistributedLock.class);
    /**
     * 需要注意多线程安全问题
     */
    private ConcurrentHashMap<String, ThreadLocal<RedisLockContext>> contextThreadLocal = new ConcurrentHashMap<>();

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
    public Boolean setLockContext(LockContext context) {
        /**
         * 通过命名空间获取多个锁，同一个命名空间一个线程只能获取一个锁
         */
        if (null == contextThreadLocal.get(context.getNamespace())) {
            synchronized (this){
                if(null == contextThreadLocal.get(context.getNamespace())){
                    contextThreadLocal.put(context.getNamespace(), new ThreadLocal<>());
                }
            }
        }
        LockContext existLockContext = contextThreadLocal.get(context.getNamespace()).get();
        if (existLockContext == null ||
                !existLockContext.getNamespace().equals(context.getNamespace())
                || LockState.RELEASE.equals(existLockContext.getLockState())) {
            RedisLockContext redisLockContext = (RedisLockContext) context;
            redisLockContext.setState(LockState.WAIT);
            redisLockContext.setThreadId(Thread.currentThread().getId());
            redisLockContext.setScheduler(new ScheduledThreadPoolExecutor(1));
            contextThreadLocal.get(redisLockContext.getNamespace()).set(redisLockContext);
            return Boolean.TRUE;
        } else {
            logger.error("The same namespace must release the lock to acquire a new lock");
            return Boolean.FALSE;
        }
    }

    /**
     * 获取锁的状态
     *
     * @return
     */
    @Override
    public LockState getLockState(String namespace) {
        return contextThreadLocal.get(namespace).get().getLockState();
    }

    /**
     * 获取锁的当前持有线程id
     */
    @Override
    public long getCurrentHoldThread(String namespace) {
        return contextThreadLocal.get(namespace).get().getThreadId();
    }

    @Override
    public void lock(String namespace) throws DistributedLockException {
        if (!tryLock(namespace, contextThreadLocal.get(namespace).get().getTimeout() << 1, TimeUnit.SECONDS)) {
            throw new DistributedLockException("Distributed lock acquisition timeout.");
        }
    }

    @Override
    public void lockInterruptibly(String namespace) throws InterruptedException {

    }


    @Override
    public boolean tryLock(String namespace) {

        /**
         * 1、需要判断当前线程是否已经获取到锁
         * 2、尝试获取锁
         */
        RedisLockContext context = contextThreadLocal.get(namespace).get();
        if (LockState.LOCKING.equals(context.getLockState())) {
            return true;
        }
        boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(context.getNamespaceAndKey(),
                context.getValue(), context.getTimeout(), TimeUnit.SECONDS);
        if (isLock) {
            /**
             * 获取到锁
             */
            context.setState(LockState.LOCKING);
            /**
             * 开启锁监测
             */
            context.getScheduler().scheduleWithFixedDelay(new Monitor(context),
                    context.getTimeout() >> 1, context.getTimeout() >> 1, TimeUnit.SECONDS);
        }
        return isLock;
    }


    @Override
    public boolean tryLock(String namespace, long time, TimeUnit unit) throws DistributedLockException {
        /**
         * 如果未获取到锁则开始自旋
         */
        long sleep = LockConstant.SPIN_TIME;
        long timeout = TimeUnit.MILLISECONDS.convert(time, unit);
        while (!tryLock(namespace)) {
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
    public void unlock(String namespace) throws DistributedLockLoseException {
        //先停止任务
        RedisLockContext context = contextThreadLocal.get(namespace).get();
        context.getScheduler().shutdownNow();
        contextThreadLocal.get(namespace).remove();
        if (context.getLockState().equals(LockState.LOCKING)) {
            String value = stringRedisTemplate.opsForValue().get(context.getNamespaceAndKey());
            /**
             * 保证持有人解锁 , 此处需要修改为保证原子性的操作 TODO 需要修改为保持原子性
             */
            if (StringUtils.hasLength(value) && value.equals(context.getValue())
                    && stringRedisTemplate.delete(context.getNamespaceAndKey())) {
                /**
                 * 释放锁
                 */
                context.setState(LockState.RELEASE);
                logger.info("Lock released successfully.");
            } else {
                /**
                 * 出现锁丢失
                 */
                throw new DistributedLockLoseException("Distributed lock lost.");
            }
        }
    }


    /**
     * 监测任务
     */
    private class Monitor implements Runnable {
        private RedisLockContext context;

        Monitor(RedisLockContext context) {
            this.context = context;
        }

        @Override
        public void run() {
            monitorTask(this.context);
        }

        /**
         * 监测锁的状态，给锁进行续时
         */
        private Boolean monitorTask(RedisLockContext context) {
            /**
             * 此处需要修改为保持原子性的操作， 存在锁丢失的可能 ，例如：redis 主从 、哨兵模式 TODO 需要修改为保持原子性
             */
            if (stringRedisTemplate.hasKey(context.getNamespaceAndKey())) {
                Date now = new Date();
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(now);
                calendar.add(Calendar.SECOND, (int) context.getTimeout());
                return stringRedisTemplate.expireAt(context.getNamespaceAndKey(), calendar.getTime());
            }
            return false;
        }
    }
}
