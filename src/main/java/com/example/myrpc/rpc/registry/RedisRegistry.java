package com.example.myrpc.rpc.registry;

import java.util.List;

public class RedisRegistry implements ServiceRegistry {

    private RegistryConfig registryConfig;

    public RedisRegistry(RegistryConfig registryConfig) {
        this.registryConfig = registryConfig;
    }

    @Override
    public void init() {
        throw new UnsupportedOperationException("Redis registry is not supported");
    }

    @Override
    public void registerService(ServiceMetaData serviceMetaData) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<ServiceMetaData> fetchService(String serviceName) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
