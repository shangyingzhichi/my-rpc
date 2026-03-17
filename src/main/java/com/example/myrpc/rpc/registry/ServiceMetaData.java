package com.example.myrpc.rpc.registry;

import lombok.Data;

/**
 * 注册中心元数据
 */
@Data
public class ServiceMetaData {
    private String serviceName;
    private String host;
    private int port;
}
