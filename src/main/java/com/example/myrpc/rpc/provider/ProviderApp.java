package com.example.myrpc.rpc.provider;

public class ProviderApp {
    public static void main(String[] args) {
        ProviderServer providerServer = new ProviderServer(9999);
        providerServer.start();
    }
}
