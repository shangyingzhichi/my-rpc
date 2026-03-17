package com.example.myrpc.rpc.loadbalance;

import lombok.Data;
import lombok.Getter;

@Getter
public enum LoadBalanceType {
    ROUND_ROBIN("RoundRobin"),
    RANDOM("Random");

    private String type;

    LoadBalanceType(String type) {
        this.type = type;
    }
}
