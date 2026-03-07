package com.example.myrpc.rpc.provider;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 注册表
 */
public class ServiceRegistry {

    private Map<String, ServiceInstanceWrapper> serviceMap = new ConcurrentHashMap<>();

    public <I> void register(Class<I> interfaceClazz, I instance) {
        if (!interfaceClazz.isInterface()) {
            throw new IllegalArgumentException("只能注册接口");
        }
        serviceMap.computeIfAbsent(interfaceClazz.getName(), v -> new ServiceInstanceWrapper<I>(interfaceClazz, instance));
    }

    public ServiceInstanceWrapper findService(String serviceName) {
        return serviceMap.get(serviceName);
    }


    /**
     * 服务实例的包装类
     */
    public static  class ServiceInstanceWrapper<I> {
        private Class<I> interfaceClazz;
        private I instance;

        public ServiceInstanceWrapper(Class<I> interfaceClazz, I instance) {
            this.interfaceClazz = interfaceClazz;
            this.instance = instance;
        }

        /**
         * 方法调用（这里使用反射，实际dubbo中优化为了if-else）
         */
        public Object invoke(String methodName, Class<?>[] paramTypes, Object[] params) throws Exception {
            // 注意：是从接口获取的方法，而不是实例
            Method method = interfaceClazz.getMethod(methodName, paramTypes);
            return method.invoke(instance, params);
        }
    }
}
