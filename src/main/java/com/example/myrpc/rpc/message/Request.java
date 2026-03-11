package com.example.myrpc.rpc.message;

import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class Request {
    // 全局递增计数器
    private static final AtomicInteger counter = new AtomicInteger(0);
    // 用来和response匹配
    private int requestId = counter.incrementAndGet();
    /**
     * 服务名（接口名）
     */
    private String serviceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数类型
     */
    private Class<?>[] paramTypes;
    /**
     * 参数
     */
    private Object[] params;
}
