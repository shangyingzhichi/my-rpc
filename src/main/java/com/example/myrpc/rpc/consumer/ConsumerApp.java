package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.registry.RegistryConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsumerApp {

    public static void main(String[] args) throws InterruptedException {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setConnectString("127.0.0.1:2181");
        registryConfig.setRegistryType(RegistryConfig.RegistryType.ZOOKEEPER);

        ConsumerProperties consumerProperties = new ConsumerProperties();
        consumerProperties.setRegistryConfig(registryConfig);


        ConsumerProxyFactory proxyFactory = new ConsumerProxyFactory(consumerProperties);

        IAdd proxy = proxyFactory.createProxy(IAdd.class);
        while(true) {
            try {
                proxy.add(1, 1);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Thread.sleep(3* 1000);
        }
    }
}
