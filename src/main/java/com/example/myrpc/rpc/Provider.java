package com.example.myrpc.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

public class Provider {


    public static void main(String[] args) throws InterruptedException {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(new NioEventLoopGroup(1), new NioEventLoopGroup(4))
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new LineBasedFrameDecoder(1024))
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String message) throws Exception {
                                        // add,a,b
                                        String[] split = message.split(",");
                                        String method = split[0];
                                        int a = Integer.parseInt(split[1]);
                                        int b = Integer.parseInt(split[2]);
                                        int result = add(a, b);

                                        channelHandlerContext.writeAndFlush(result + "\n");
                                    }
                                });
                    }
                });

        // 绑定端口，并同步等待
        ChannelFuture syncFuture = serverBootstrap.bind(9999).sync();
        System.out.println(syncFuture.isSuccess() ? "Provider启动成功" : "Provider启动失败");

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
