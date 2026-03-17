package com.example.myrpc.rpc.registry;

import java.util.List;

/**
 * 注册中心接口类
 */
public interface ServiceRegistry {

    void init();

    void registerService(ServiceMetaData serviceMetaData);

    List<ServiceMetaData> fetchService(String serviceName) throws Exception;

}
