package com.zenghm.distributed.lock.core.zookeeper;

import com.zenghm.distributed.lock.core.LockConstant;
import com.zenghm.distributed.lock.core.LockContext;
import com.zenghm.distributed.lock.core.LockState;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;

/**
 * @author Airlen
 * @date 2021/3/16
 * @description xxx
 */
public class ZookeeperLockContext implements LockContext {

    private String namespace;
    private String key;
    private String value;
    private long timeout;
    private long threadId;
    private LockState state;

    //zookeeper 创建的临时节点路径
    private InterProcessMutex mutex;

    public ZookeeperLockContext(String namespace, String key, long timeout) {
        this.namespace = namespace;
        this.key = key;
        this.timeout = timeout;
        //this.value =
    }

    public String getNamespaceAndKey() {
        return String.format("%s/%s/%s", LockConstant.ROOT_PATH, getNamespace(), getKey());
    }

    public void setValue(String value) {
        this.value = value;
    }

    public InterProcessMutex getMutex() {
        return mutex;
    }

    public void setMutex(InterProcessMutex mutex) {
        this.mutex = mutex;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public void setState(LockState state) {
        this.state = state;
    }

    @Override
    public String getNamespace() {
        return namespace;
    }

    @Override
    public String getKey() {
        return key;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    @Override
    public long getThreadId() {
        return threadId;
    }

    @Override
    public LockState getLockState() {
        return state;
    }
}
