package com.example.myrpc.rpc.provider;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.provider.impl.AddImpl;
import com.example.myrpc.rpc.registry.RegistryConfig;

public class ProviderApp {
    public static void main(String[] args) {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setConnectString("127.0.0.1:2181");
        registryConfig.setRegistryType(RegistryConfig.RegistryType.ZOOKEEPER);

        ProviderServer providerServer = new ProviderServer("127.0.0.1", 9998, registryConfig);
        providerServer.registerServiceLocal(IAdd.class, new AddImpl() );
        providerServer.start();
    }
}
