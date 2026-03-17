package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.codec.RequestMessageEncoder;
import com.example.myrpc.rpc.exception.RpcException;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.message.Response;
import com.example.myrpc.rpc.registry.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * consumer代理工厂
 */
@Slf4j
public class ConsumerProxyFactory {
    /**
     * 异步响应（在途请求）的标准处理，<requestId, 异步响应>
     */
    private final Map<Integer, CompletableFuture<Response>> inFightRequestTable = new ConcurrentHashMap<>();
    /**
     * 连接管理器
     */
    private final ConnectionManager connectionManager = new ConnectionManager(createBootstrap());

    /**
     * 注册中心
     */
    private final ServiceRegistry serviceRegistry;

    public ConsumerProxyFactory(RegistryConfig registryConfig) {
        this.serviceRegistry = new DefaultRegistry(registryConfig);
        serviceRegistry.init();
    }

    private Bootstrap createBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new MessageDecoder())  // byte[] -> Message(response)
                                .addLast(new RequestMessageEncoder())  // message(request) -> byte[]
                                .addLast(new SimpleChannelInboundHandler<Response>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Response response) throws Exception {
                                        CompletableFuture<Response> completableFuture = inFightRequestTable.remove(response.getRequestId());
                                        if (completableFuture == null) {
                                            log.error("【{}】无法找到对应的在途请求", response.getRequestId());
                                            return;
                                        }
                                        // 响应结束
                                        completableFuture.complete(response);
                                    }
                                });
                    }
                });
        return bootstrap;
    }

    /**
     * 创建代理对象
     */
    public <I> I createProxy(Class<I> interfaceClass) {
        Object proxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClass},
                new ConsumerInvocationHandler<>(interfaceClass)
        );
        return (I) proxyInstance;
    }


    public class ConsumerInvocationHandler<I> implements InvocationHandler {
        private final Class<I> clazz;
        public  ConsumerInvocationHandler(Class<I> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object对象的方法
            if (method.getDeclaringClass() == Object.class) {
                if (method.getName().equals("equals")) {
                    return proxy == args[0];
                } else if (method.getName().equals("hashCode")) {
                    return System.identityHashCode(proxy);
                } else if (method.getName().equals("toString")) {
                    return proxy.getClass().getName();
                } else {
                    throw new RpcException("不允许调用该对象的方法：" + method.getName());
                }
            }

            // 异步容器，放provider的返回值
            CompletableFuture<Response> resultFuture = new CompletableFuture<>();

            // 连接Provider
            try {
                List<ServiceMetaData> serviceMetaDataList = serviceRegistry.fetchService(clazz.getName());
                if (serviceMetaDataList == null || serviceMetaDataList.isEmpty()) {
                    throw new RpcException(String.format("service【%s】对应的provider为空", clazz.getName()));
                }
                ServiceMetaData serviceMetaData = serviceMetaDataList.get(0);
                Channel channel = connectionManager.getChannel(serviceMetaData.getHost(), serviceMetaData.getPort());
                if (channel == null) {
                    throw new RpcException("建立连接失败");
                }
                // 发起请求
                Request request = new Request();
                request.setServiceName(clazz.getName());
                request.setMethodName(method.getName());
                request.setParamTypes(method.getParameterTypes());
                request.setParams(args);
                // 先放到在途请求中，防止通信过快，无法找到response
                inFightRequestTable.put(request.getRequestId(), resultFuture);

                channel.writeAndFlush(request).addListener((future) -> {
                    if (!future.isSuccess()) {
                        inFightRequestTable.remove(request.getRequestId());
                        resultFuture.completeExceptionally(future.cause());
                    }
                });

                // 等待获取Provider结果
                // 超时5s
                Response response = resultFuture.get(5, TimeUnit.SECONDS);

                // 接收响应
                if (Response.isSuccess(response)) {
                    log.info("成功获取Provider结果：" + response.getResult());
                    return response.getResult();
                } else {
                    throw new RpcException(response.getMsg());
                }
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
