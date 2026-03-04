package com.example.myrpc.rpc;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.concurrent.CompletableFuture;

public class Consumer {

    public static void add(int a, int b) throws InterruptedException {
        // 异步容器，放provider的返回值
        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline()
                                .addLast(new LineBasedFrameDecoder(1024))
                                .addLast(new StringDecoder())
                                .addLast(new StringEncoder())
                                .addLast(new SimpleChannelInboundHandler<String>() {
                                    @Override
                                    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String msg) throws Exception {
                                        Integer result = Integer.valueOf(msg);
                                        resultFuture.complete(result);
                                    }
                                });
                    }
                });

        // 连接Provider
        ChannelFuture syncFuture = bootstrap.connect("localhost", 9999).sync();
        syncFuture.channel().writeAndFlush(String.format("add,%d,%d\n", a, b));
        // 等待获取Provider结果
        Integer result = resultFuture.join();
        System.out.println("成功获取Provider结果：" + result);
    }


    public static void main(String[] args) throws InterruptedException {
        // test
        add(1,2);
    }
}
