package com.zenghm.distributed.lock.example.controller;

import com.zenghm.distributed.lock.core.DefaultDistributedLock;
import com.zenghm.distributed.lock.core.redis.RedisLockContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Airlen
 * @date 2021/3/23
 * @description xxx
 */
@RestController
@RequestMapping("distributedlock")
public class DistributedLockController {
    private Integer state = 50;
    private final Logger logger = LoggerFactory.getLogger(DistributedLockController.class);
    @Autowired
    DefaultDistributedLock distributedLockFactory;

    @RequestMapping("test")
    @ResponseBody
    public String doDistributedLockTest(){
        RedisLockContext context = new RedisLockContext("test","1234",3);
        Object obj = distributedLockFactory.tryLock(context, context1 -> {
            logger.info("redis lock acquire success.");
            if(state>0){
                logger.info("state:"+(--state));
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new Object();
        });
        if(obj!=null){
            logger.info("execute success.");
        }else {
            logger.info("execute fail.");
        }
        return "200";
    }
}
