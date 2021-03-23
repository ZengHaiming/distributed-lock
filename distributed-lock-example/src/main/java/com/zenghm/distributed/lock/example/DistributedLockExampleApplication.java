package com.zenghm.distributed.lock.example;

import com.zenghm.distributed.lock.core.EnableDistributedLock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author Airlen
 * @date 2021/3/17
 * @description xxx
 */
@EnableDistributedLock
@SpringBootApplication
public class DistributedLockExampleApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributedLockExampleApplication.class, args);
    }
}
