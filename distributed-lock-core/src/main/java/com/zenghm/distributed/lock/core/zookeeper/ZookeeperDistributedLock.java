package com.zenghm.distributed.lock.core.zookeeper;


import com.zenghm.distributed.lock.core.DistributedLock;
import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import com.zenghm.distributed.lock.core.exception.DistributedLockException;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.zookeeper.ZookeeperProperties;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
@Service
public class ZookeeperDistributedLock implements DistributedLock , InitializingBean {
    private Logger logger = LoggerFactory.getLogger(ZookeeperDistributedLock.class);
    private ConcurrentHashMap<String, ThreadLocal<ZookeeperLockContext>> contextThreadLocal = new ConcurrentHashMap<>();

    @Autowired
    private ZookeeperProperties zookeeperProperties;
    private CuratorFramework curatorFramework;
    /**
     * 是否能进行处理
     *
     * @param lockContext
     * @return
     */
    @Override
    public boolean handler(LockContext lockContext) {
        return lockContext instanceof ZookeeperLockContext;
    }

    @Override
    public void afterPropertiesSet() {
        if(null == curatorFramework){
            synchronized (this){
                if(null== curatorFramework){
                    curatorFramework = CuratorFrameworkFactory.newClient(zookeeperProperties.getConnectString(),
                            (int) zookeeperProperties.getSessionTimeout().toMillis(), (int) zookeeperProperties.getConnectionTimeout().toMillis(),
                            new ExponentialBackoffRetry(1000,3));
                }
            }
        }
        curatorFramework.start();
    }


    @Override
    public void lock(String namespace) throws DistributedLockException{
        if(!tryLock(namespace,contextThreadLocal.get(namespace).get().getTimeout() << 1, TimeUnit.SECONDS)){
            throw new DistributedLockException("Distributed lock acquisition timeout.");
        }
    }

    @Override
    public void lockInterruptibly(String namespace) throws DistributedLockException {

    }


    @Override
    public boolean tryLock(String namespace) {
        ZookeeperLockContext lockContext = contextThreadLocal.get(namespace).get();
        if(LockState.LOCKING.equals(lockContext.getLockState())){
            return true;
        }
        try {
            lockContext.getMutex().acquire();
            return true;
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return false;
        }
    }


    @Override
    public boolean tryLock(String namespace,long time, TimeUnit unit) {
        ZookeeperLockContext lockContext = contextThreadLocal.get(namespace).get();
        if(LockState.LOCKING.equals(lockContext.getLockState())){
            return true;
        }
        try {
            return lockContext.getMutex().acquire(time,unit);
        } catch (Exception e) {
            logger.error(e.getMessage(),e);
            return false;
        }
    }

    @Override
    public void unlock(String namespace) {
        try {
            ZookeeperLockContext lockContext = contextThreadLocal.get(namespace).get();
            contextThreadLocal.get(namespace).remove();
            if(lockContext.getMutex() != null && lockContext.getMutex().isAcquiredInThisProcess()){
                lockContext.getMutex().release();
                curatorFramework.delete().inBackground().forPath(lockContext.getNamespaceAndKey());
                logger.info("Thread:"+Thread.currentThread().getId()+" release distributed lock  success");
            }
        }catch (Exception e){
            logger.info("Thread:"+Thread.currentThread().getId()+" release distributed lock  exception="+e);
        }
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
            ZookeeperLockContext zookeeperLockContext = (ZookeeperLockContext) context;
            zookeeperLockContext.setState(LockState.WAIT);
            zookeeperLockContext.setThreadId(Thread.currentThread().getId());
            zookeeperLockContext.setMutex(new InterProcessMutex(curatorFramework,zookeeperLockContext.getNamespaceAndKey()));
            contextThreadLocal.get(zookeeperLockContext.getNamespace()).set(zookeeperLockContext);
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

}
