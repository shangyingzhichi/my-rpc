package com.example.myrpc.rpc.loadbalance;

import com.example.myrpc.rpc.registry.ServiceMetaData;

import java.util.List;

/**
 * 负载均衡
 */
public interface LoadBalancer {

    ServiceMetaData choose(List<ServiceMetaData> serviceMetaDataList);
}
