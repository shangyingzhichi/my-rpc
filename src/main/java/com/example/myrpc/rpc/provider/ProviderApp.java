package com.example.myrpc.rpc.provider;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.provider.impl.AddImpl;

public class ProviderApp {
    public static void main(String[] args) {
        ProviderServer providerServer = new ProviderServer(9999);
        providerServer.registerService(IAdd.class, new AddImpl() );
        providerServer.start();
    }
}
