package com.example.myrpc.rpc.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 注册中心（zk实现版）
 */
@Slf4j
public class ZkServiceRegistry implements ServiceRegistry {

    public static final String BASE_PATH = "/test";

    private ServiceDiscovery<ServiceMetaData> discovery;
    private final RegistryConfig registryConfig;
    private CuratorFramework client;

    public ZkServiceRegistry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }


    @Override
    public void init() {
        // 初始化逻辑不能放在构造函数中
        // 1. 复杂操作导致new异常，无法重试
        client = CuratorFrameworkFactory.builder()
                .connectString(registryConfig.getConnectString())
                .sessionTimeoutMs(10 * 1000)
                .connectionTimeoutMs(5 * 1000)
                .retryPolicy(new ExponentialBackoffRetry(1000, 3))
                .build();
        client.start();


        discovery = ServiceDiscoveryBuilder.builder(ServiceMetaData.class)
                .client(client)
                .basePath(BASE_PATH)
                .serializer(new JsonInstanceSerializer<>(ServiceMetaData.class))
                .build();

        try {
            discovery.start();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void registerService(ServiceMetaData serviceMetaData) {
        ServiceInstance<ServiceMetaData> serviceInstance = toServiceInstance(serviceMetaData);
        try {
            discovery.registerService(serviceInstance);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<ServiceMetaData> fetchService(String serviceName) throws Exception {
        // 异常抛到上级处理
        return discovery.queryForInstances(serviceName)
             .stream()
             .map(ServiceInstance::getPayload)
             .collect(Collectors.toList());
    }


    private ServiceInstance<ServiceMetaData> toServiceInstance(ServiceMetaData serviceMetaData) {
        ServiceInstance<ServiceMetaData> serviceInstance;
        try {
            serviceInstance = ServiceInstance.<ServiceMetaData>builder()
                    .name(serviceMetaData.getServiceName())
                    .address(serviceMetaData.getHost())
                    .port(serviceMetaData.getPort())
                    .payload(serviceMetaData)
                    .build();
        } catch (Exception e) {
            log.error("toServiceInstance [{}] error", serviceMetaData.getServiceName(), e);
            throw new RuntimeException(e);
        }
        return serviceInstance;
    }


}
