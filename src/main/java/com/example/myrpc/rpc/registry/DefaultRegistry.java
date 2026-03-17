package com.example.myrpc.rpc.registry;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 装饰器（isA && hasA）
 */
@Slf4j
public class DefaultRegistry implements ServiceRegistry {

    private final RegistryConfig registryConfig;
    private ServiceRegistry serviceRegistry;
    private Map<String, List<ServiceMetaData>> cache = new ConcurrentHashMap<>();

    public DefaultRegistry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    @Override
    public void init() {
        serviceRegistry = RegistryFactory.create(registryConfig);
        serviceRegistry.init();
    }

    @Override
    public void registerService(ServiceMetaData serviceMetaData) {
        log.info("Registering service[{}] to Registry[{}]: ", serviceMetaData.getServiceName(), serviceRegistry.getClass().getSimpleName());
        serviceRegistry.registerService(serviceMetaData);
    }

    @Override
    public List<ServiceMetaData> fetchService(String serviceName) {
        log.info("Fetching service[{}] from Registry[{}]", serviceName, serviceRegistry.getClass().getSimpleName());
        // 教科书般的错误处理：下级异常抛上来
        try {
            List<ServiceMetaData> serviceMetaData = serviceRegistry.fetchService(serviceName);
            // 请求成功（不管结果如何），缓存
            cache.put(serviceName, serviceMetaData);
            return serviceMetaData;
        } catch (Exception e) {
            log.error("Fetching service[{}] from Registry[{}] error, error message: {}", serviceName, serviceRegistry.getClass().getSimpleName(), e.getMessage());
            // 尝试从缓存拿（注册中心异常，但provider正常的情况）
            return cache.getOrDefault(serviceName, new ArrayList<>());
        }
    }
}
