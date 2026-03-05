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

public class ProviderServer {

    private final int port;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;


    public ProviderServer(int port) {
        this.port = port;
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
                                    .addLast(new SimpleChannelInboundHandler<Request>() {
                                        @Override
                                        protected void channelRead0(ChannelHandlerContext channelHandlerContext, Request request) throws Exception {
                                            // 接收请求
                                            System.out.println("Server received: " + request);

                                            // 返回响应
                                            Response response = new Response();
                                            response.setResult(add(1, 2));
                                            channelHandlerContext.writeAndFlush(response);
                                        }
                                    });
                        }
                    });
            // 绑定端口，并同步等待
            ChannelFuture syncFuture = serverBootstrap.bind(port).sync();
            System.out.println("Provider启动成功");
        }catch (Exception e) {
            throw new RuntimeException("Provider启动异常", e);
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






    public static int add(int a, int b) {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return a + b;
    }

}
