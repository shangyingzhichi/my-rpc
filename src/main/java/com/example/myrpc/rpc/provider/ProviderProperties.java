package com.example.myrpc.rpc.provider;

import com.example.myrpc.rpc.registry.RegistryConfig;
import lombok.Data;

@Data
public class ProviderProperties {
    private String host;
    private int port;
    private int workerThreadNum = 4;
    private RegistryConfig registryConfig;
}
