package com.example.myrpc.rpc.consumer;

import com.example.myrpc.rpc.api.IAdd;
import com.example.myrpc.rpc.codec.MessageDecoder;
import com.example.myrpc.rpc.exception.RpcException;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.codec.RequestMessageEncoder;
import com.example.myrpc.rpc.message.Response;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class Consumer implements IAdd {
    /**
     * 异步响应（在途请求）的标准处理，<requestId, 异步响应>
     */
    private final Map<Integer, CompletableFuture<?>> inFightRequestTable = new ConcurrentHashMap<>();

    private final ConnectionManager connectionManager = new ConnectionManager(createBootstrap());

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
                                        CompletableFuture completableFuture = inFightRequestTable.remove(response.getRequestId());
                                        // 接收响应
                                        if (Response.isSuccess(response)) {
                                            Integer result = Integer.valueOf(String.valueOf(response.getResult()));
                                            completableFuture.complete(result);
                                        } else {
                                            completableFuture.completeExceptionally((new RpcException(response.getMsg())));
                                        }
                                    }
                                });
                    }
                });
        return bootstrap;
    }


    @Override
    public int add(int a, int b) {
        // 异步容器，放provider的返回值
        CompletableFuture<Integer> resultFuture = new CompletableFuture<>();

        // 连接Provider
        try {
            Channel channel = connectionManager.getChannel("localhost", 9999);
            if (channel == null) {
                throw new RpcException("建立连接失败");
            }
            // 发起请求
            Request request = new Request();
            request.setServiceName(IAdd.class.getName());
            request.setMethodName("add");
            request.setParamTypes(new Class[]{int.class, int.class});
            request.setParams(new Object[]{a, b});

            channel.writeAndFlush(request).addListener((future) -> {
                if (future.isSuccess()) {
                    // 发送请求成功，则缓存异步响应
                    inFightRequestTable.put(request.getRequestId(), resultFuture);
                }
            });

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
