package com.zenghm.distributed.lock.core;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Documented
@Inherited
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({DistributedLockConfig.class})
public @interface EnableDistributedLock {
}
