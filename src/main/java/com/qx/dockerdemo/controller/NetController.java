package com.qx.dockerdemo.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NetController {
    private final StringRedisTemplate redisTemplate;

    public NetController(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/redis")
    public String redis() {
        redisTemplate.opsForValue().set("docker:test", "hello redis");
        return redisTemplate.opsForValue().get("docker:test");
    }
}
