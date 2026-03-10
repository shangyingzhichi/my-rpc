package com.example.myrpc.rpc.provider;

import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.message.Response;
import com.example.myrpc.rpc.codec.ResponseMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProviderServer {

    private final int port;

    private final ServiceRegistry serviceRegistry;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    public ProviderServer(int port) {
        this.port = port;
        serviceRegistry = new ServiceRegistry();
    }

    public <I> void registerService(Class<I> interfaceClass, I instance) {
        serviceRegistry.register(interfaceClass, instance);
    }


    public void start() {
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(4);

        try {
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
            ChannelFuture syncFuture = serverBootstrap.bind(port).sync();
            System.out.println("Provider启动成功");
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
            ServiceRegistry.ServiceInstanceWrapper service = serviceRegistry.findService(request.getServiceName());
            if (service == null) {
                log.info("find service by name error: {}", request.getServiceName());
                response = Response.fail(Response.ERROR, String.format("service not found: %s", request.getServiceName()));
                channelHandlerContext.writeAndFlush(response);
            }
            try {
                // 2. 调用方法
                Object result = service.invoke(request.getMethodName(), request.getParamTypes(), request.getParams());
                log.info("调用服务【{}】的方法【{}】成功，结果为：【{}】", request.getServiceName(), request.getMethodName(), result);
                channelHandlerContext.writeAndFlush(Response.success(result));
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                channelHandlerContext.writeAndFlush(Response.fail(Response.ERROR, e.getMessage()));
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
