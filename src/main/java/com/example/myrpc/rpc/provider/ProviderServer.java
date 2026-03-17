package com.example.myrpc.rpc.provider;

import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.message.Response;
import com.example.myrpc.rpc.codec.ResponseMessageEncoder;
import com.example.myrpc.rpc.registry.*;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProviderServer {

    // 本地注册表
    private final LocalServiceRegistry localServiceRegistry;
    // provider配置信息
    private final ProviderProperties providerProperties;
    // 注册中心
    private final ServiceRegistry serviceRegistry;
    // EventLoop线程组
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;



    public ProviderServer(ProviderProperties providerProperties) {
        this.providerProperties = providerProperties;
        localServiceRegistry = new LocalServiceRegistry();
        serviceRegistry = new DefaultRegistry(providerProperties.getRegistryConfig());
    }

    /**
     * 向本地注册表注册服务
     */
    public <I> void registerServiceLocal(Class<I> interfaceClass, I instance) {
        localServiceRegistry.register(interfaceClass, instance);
    }

    /**
     * 将本地注册表的所有服务，注册到注册中心
     */
    public void registerLocalToRegistry() {
        log.info("Registering local service registry");
        localServiceRegistry.getAllServices().forEach(service -> {
            ServiceMetaData serviceMetaData = new ServiceMetaData();
            serviceMetaData.setServiceName(service);
            serviceMetaData.setHost(providerProperties.getHost());
            serviceMetaData.setPort(providerProperties.getPort());
            serviceRegistry.registerService(serviceMetaData);
        });
        log.info("Register local service registry completed");
    }


    public void start() {
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup(providerProperties.getWorkerThreadNum());

        try {
            // 注册中心初始化
            serviceRegistry.init();
            // 初始化ServerBootstrap
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new MessageDecoder())  // byte[] -> Message(request)
                                    .addLast(new ResponseMessageEncoder())  // message(request) -> byte[]
                                    .addLast(new RequestHandler());
                        }
                    });
            // 绑定端口，并同步等待
            ChannelFuture syncFuture = serverBootstrap.bind(providerProperties.getHost(), providerProperties.getPort()).sync();
            System.out.println("Provider启动成功");
            // 将服务注册到注册中心
            registerLocalToRegistry();
        } catch (Exception e) {
            throw new RuntimeException("Provider启动异常", e);
        }
    }

    public class RequestHandler extends SimpleChannelInboundHandler<Request> {

        @Override
        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) throws Exception {
            // 接收请求
            log.info("Server received request: {}", request);
            Response response;

            // 1. 获取服务
            LocalServiceRegistry.ServiceInstanceWrapper service = localServiceRegistry.findService(request.getServiceName());
            if (service == null) {
                log.info("find service by name error: {}", request.getServiceName());
                response = Response.fail(request.getRequestId(), Response.ERROR, String.format("service not found: %s", request.getServiceName()));
                channelHandlerContext.writeAndFlush(response);
            }
            try {
                // 2. 调用方法
                Object result = service.invoke(request.getMethodName(), request.getParamTypes(), request.getParams());
                log.info("调用服务【{}】的方法【{}】成功，结果为：【{}】", request.getServiceName(), request.getMethodName(), result);
                channelHandlerContext.writeAndFlush(Response.success(request.getRequestId(), result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                channelHandlerContext.writeAndFlush(Response.fail(request.getRequestId(), Response.ERROR, e.getMessage()));
            }
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

    public void stop() {
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }


}
