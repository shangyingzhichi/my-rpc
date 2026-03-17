package com.example.myrpc.rpc.loadbalance;

import com.example.myrpc.rpc.registry.ServiceMetaData;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 轮询
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public ServiceMetaData choose(List<ServiceMetaData> serviceMetaDataList) {
        int index = counter.getAndIncrement() % serviceMetaDataList.size();
        return serviceMetaDataList.get(Math.abs(index));
    }
}
