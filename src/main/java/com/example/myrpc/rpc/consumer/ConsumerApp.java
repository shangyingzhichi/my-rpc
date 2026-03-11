package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.api.IAdd;

public class ConsumerApp {

    public static void main(String[] args) throws InterruptedException {
        ConsumerProxyFactory proxyFactory = new ConsumerProxyFactory();

        IAdd proxy = proxyFactory.createProxy(IAdd.class);
        proxy.add(1, 2);
        proxy.add(2, 3);
    }
}
