package com.example.myrpc.rpc.registry;

public class RegistryFactory {

    public static ServiceRegistry create(RegistryConfig registryConfig) {
        switch (registryConfig.getRegistryType()) {
            case ZOOKEEPER:
                return new ZkServiceRegistry(registryConfig);
            case REDIS:
                return new RedisRegistry(registryConfig);
            default: throw new IllegalArgumentException("Unknown registry type: " + registryConfig.getRegistryType());
        }
    }
}
