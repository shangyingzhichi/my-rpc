package com.example.myrpc.rpc.loadbalance;

public class LoadBalanceFactory {

    public static LoadBalancer create(LoadBalanceType loadBalanceType) {
        switch (loadBalanceType) {
            case RANDOM: return new RandomLoadBalancer();
            case ROUND_ROBIN: return new RoundRobinLoadBalancer();
            default: throw new IllegalArgumentException("LoadBalanceType not supported: " + loadBalanceType);
        }
    }
}
