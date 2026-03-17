package com.example.myrpc.rpc.loadbalance;

import com.example.myrpc.rpc.registry.ServiceMetaData;

import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 随机
 */
public class RandomLoadBalancer implements LoadBalancer {
    private final Random random = new Random();

    @Override
    public ServiceMetaData choose(List<ServiceMetaData> serviceMetaDataList) {
        int index = random.nextInt(0, serviceMetaDataList.size());
        return serviceMetaDataList.get(index);
    }
}
