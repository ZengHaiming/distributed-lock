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


    RedisLockContext context;

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
        this.context = (RedisLockContext) context;
        this.context.setState(LockState.WAIT);
        this.context.setThreadId(Thread.currentThread().getId());
        this.context.setScheduler(new ScheduledThreadPoolExecutor(1));
    }

    /**
     * 获取锁的状态
     *
     * @return
     */
    @Override
    public LockState getLockState() {
        return context.getLockState();
    }

    /**
     * 获取锁的当前持有线程id
     */
    @Override
    public long getCurrentHoldThread() {
        return context.getThreadId();
    }

    @Override
    public void lock() throws DistributedLockException {
        if (!tryLock(this.context.getTimeout() << 1, TimeUnit.SECONDS)) {
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
        boolean isLock = stringRedisTemplate.opsForValue().setIfAbsent(this.context.getKey(),
                this.context.getValue(), this.context.getTimeout(), TimeUnit.SECONDS);
        if (isLock) {
            /**
             * 获取到锁
             */
            this.context.setState(LockState.LOCKING);
            /**
             * 开启锁监测
             */
            this.context.getScheduler().scheduleWithFixedDelay(this::monitorTask,
                    this.context.getTimeout() >> 1, this.context.getTimeout() >> 1, TimeUnit.SECONDS);
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
        this.context.getScheduler().shutdownNow();
        String value = stringRedisTemplate.opsForValue().get(this.context.getKey());
        /**
         * 保证持有人解锁 , 此处需要修改为保证原子性的操作
         */
        if(!StringUtils.hasLength(value)&&value.equals(this.context.getValue())){
            stringRedisTemplate.delete(this.context.getKey());
        }else {
            /**
             * 出现锁丢失
             */
            throw new DistributedLockLoseException("Distributed lock lost.");
        }
    }


    /**
     * 监测锁的状态，给锁进行续时
     */

    private Boolean monitorTask(){
        /**
         * 此处需要修改为保持原子性的操作， 存在锁丢失的可能 ，例如：redis 主从 、哨兵模式
         */
        if (stringRedisTemplate.hasKey(this.context.getKey())) {
            Date now = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(now);
            calendar.add(Calendar.SECOND, (int) this.context.getTimeout());
            return stringRedisTemplate.expireAt(this.context.getKey(), calendar.getTime());
        }
        return false;
    }
}
