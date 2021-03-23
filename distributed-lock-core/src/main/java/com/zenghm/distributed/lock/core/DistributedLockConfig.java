package com.zenghm.distributed.lock.core;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * @author Airlen
 * @date 2021/3/23
 * @description xxx
 */
@Configuration
@ComponentScan(basePackageClasses = DistributedLockConfig.class)
public class DistributedLockConfig {
}
