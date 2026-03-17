package com.example.myrpc.rpc.registry;

import lombok.Data;
import lombok.Getter;

@Data
public class RegistryConfig {
    private String connectString;
    private RegistryType registryType;


    @Getter
    public enum RegistryType {
        ZOOKEEPER("zookeeper"), REDIS("redis");

        private final String type;
        RegistryType(String type) {
            this.type = type;
        }

    }
}
