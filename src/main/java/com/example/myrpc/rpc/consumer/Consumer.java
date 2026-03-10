package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.exception.RpcException;
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
import java.util.concurrent.TimeUnit;

public class Consumer implements IAdd {

    @Override
    public int add(int a, int b) {
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
                                        if (Response.isSuccess(response)) {
                                            Integer result = Integer.valueOf(String.valueOf(response.getResult()));
                                            resultFuture.complete(result);
                                        } else {
                                            resultFuture.completeExceptionally((new RpcException(response.getMsg())));
                                        }
                                    }
                                });
                    }
                });

        // 连接Provider
        ChannelFuture syncFuture = null;
        try {
            syncFuture = bootstrap.connect("localhost", 9999).sync();
            // 发起请求
            Request request = new Request();
            request.setServiceName(IAdd.class.getName());
            request.setMethodName("add");
            request.setParamTypes(new Class[]{int.class, int.class});
            request.setParams(new Object[]{a, b});
            syncFuture.channel().writeAndFlush(request);
            // 等待获取Provider结果
            // 超时5s
            Integer result = resultFuture.get(5, TimeUnit.SECONDS);
            System.out.println("成功获取Provider结果：" + result);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


}
