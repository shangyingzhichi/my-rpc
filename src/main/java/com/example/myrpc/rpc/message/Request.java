package com.example.myrpc.rpc.message;

import lombok.Data;

@Data
public class Request {
    /**
     * 服务名（接口名）
     */
    private String serviceName;
    /**
     * 方法名
     */
    private String methodName;
    /**
     * 参数类型
     */
    private String[] paramTypes;
    /**
     * 参数
     */
    private Object[] params;
}
