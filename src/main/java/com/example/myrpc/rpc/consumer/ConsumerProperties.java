package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.loadbalance.LoadBalanceType;
import com.example.myrpc.rpc.registry.RegistryConfig;
import lombok.Data;

@Data
public class ConsumerProperties {
    // 默认4
    private int workerThreadNum = 4;
    private int connectTimeoutMs = 5000;
    private int responseTimeoutMs = 5000;
    private LoadBalanceType loadBalanceType = LoadBalanceType.ROUND_ROBIN;
    private RegistryConfig registryConfig;
}
