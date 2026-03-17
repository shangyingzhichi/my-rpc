package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.codec.RequestMessageEncoder;
import com.example.myrpc.rpc.exception.RpcException;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.message.Response;
import com.example.myrpc.rpc.registry.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
    // 异步响应（在途请求）的标准处理，<requestId, 异步响应>
    private final Map<Integer, CompletableFuture<Response>> inFightRequestTable;
    // 连接管理器
    private final ConnectionManager connectionManager;
    // 注册中心
    private final ServiceRegistry serviceRegistry;
    // consumer配置
    private final ConsumerProperties consumerProperties;

    public ConsumerProxyFactory(ConsumerProperties consumerProperties) {
        this.consumerProperties = consumerProperties;
        // bootstrap
        Bootstrap bootstrap = createBootstrap();
        // 连接管理器
        connectionManager = new ConnectionManager(bootstrap);
        // 在途请求
        inFightRequestTable = new ConcurrentHashMap<>();
        // 注册中心
        this.serviceRegistry = new DefaultRegistry(consumerProperties.getRegistryConfig());
        serviceRegistry.init();
    }

    private Bootstrap createBootstrap() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(consumerProperties.getWorkerThreadNum()))
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, consumerProperties.getConnectTimeoutMs())
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new MessageDecoder())  // byte[] -> Message(response)
                                .addLast(new RequestMessageEncoder())  // message(request) -> byte[]
                                .addLast(new ConsumerInboundHandler());
                    }
                });
        return bootstrap;
    }

    private class ConsumerInboundHandler extends SimpleChannelInboundHandler<Response> {

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

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            log.info("客户端：【{}】连接成功", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            log.info("客户端：【{}】断开连接", ctx.channel().remoteAddress());
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            log.error("未知异常", cause);
            // 未捕获的未知异常，断开连接
            ctx.channel().close();
        }
    }

    /**
     * 创建代理对象
     */
    public <I> I createProxy(Class<I> interfaceClass) {
        Object proxyInstance = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class[]{interfaceClass},
                new ConsumerInvocationHandler(interfaceClass)
        );
        return (I) proxyInstance;
    }


    public class ConsumerInvocationHandler implements InvocationHandler {
        private final Class<?> clazz;
        public  ConsumerInvocationHandler(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // 处理Object对象的方法
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }

            // 异步容器，放provider的返回值
            CompletableFuture<Response> resultFuture = new CompletableFuture<>();
            // 连接Provider
            try {
                // 从注册中心获取连接地址
                List<ServiceMetaData> serviceMetaDataList = serviceRegistry.fetchService(clazz.getName());
                if (serviceMetaDataList == null || serviceMetaDataList.isEmpty()) {
                    throw new RpcException(String.format("service【%s】对应的provider为空", clazz.getName()));
                }
                ServiceMetaData serviceMetaData = serviceMetaDataList.get(0);
                // 根据连接地址获取连接
                Channel channel = connectionManager.getChannel(serviceMetaData.getHost(), serviceMetaData.getPort());
                if (channel == null) {
                    throw new RpcException("建立连接失败");
                }
                Request request = buildRequest(clazz, method, args);
                // 先放到在途请求中，防止通信过快，无法找到response
                inFightRequestTable.put(request.getRequestId(), resultFuture);
                // 发起请求
                channel.writeAndFlush(request).addListener((future) -> {
                    if (!future.isSuccess()) {
                        inFightRequestTable.remove(request.getRequestId());
                        resultFuture.completeExceptionally(future.cause());
                    }
                });

                // 等待获取Provider结果（超时时间内）
                Response response = resultFuture.get(consumerProperties.getResponseTimeoutMs(), TimeUnit.MILLISECONDS);

                // 接收响应
                return processResponse(response);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    private Request buildRequest(Class<?> clazz, Method method, Object[] args) {
        Request request = new Request();
        request.setServiceName(clazz.getName());
        request.setMethodName(method.getName());
        request.setParamTypes(method.getParameterTypes());
        request.setParams(args);
        return request;
    }

    private Object processResponse(Response response) {
        if (Response.isSuccess(response)) {
            log.info("成功获取Provider结果：" + response.getResult());
            return response.getResult();
        } else {
            throw new RpcException(response.getMsg());
        }
    }

    private Object invokeObjectMethod(Object proxy, Method method, Object[] args) throws Throwable {
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
}
