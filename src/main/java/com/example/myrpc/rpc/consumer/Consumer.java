package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.codec.RequestMessageEncoder;
import com.example.myrpc.rpc.message.Response;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.concurrent.CompletableFuture;

public class Consumer {

    public void add(int a, int b) throws InterruptedException {
        // 异步容器，放provider的返回值
        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

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
                                        // 接收响应
                                        Integer result = Integer.valueOf(String.valueOf(response.getResult()));
                                        resultFuture.complete(result);
                                    }
                                });
                    }
                });

        // 连接Provider
        ChannelFuture syncFuture = bootstrap.connect("localhost", 9999).sync();
        // 发起请求
        Request request = new Request();
        request.setServiceName("service");
        request.setMethodName("method");
        request.setParamTypes(new String[]{"int", "int"});
        request.setParams(new Object[]{1, 2});
        syncFuture.channel().writeAndFlush(request);
        // 等待获取Provider结果
        Integer result = resultFuture.join();
        System.out.println("成功获取Provider结果：" + result);
    }


}
