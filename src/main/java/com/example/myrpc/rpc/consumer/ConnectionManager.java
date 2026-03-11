package com.example.myrpc.rpc.consumer;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 连接管理
 */
@Slf4j
public class ConnectionManager {
    // 维护长连接（防止v为null，使用Optional包装）
    private static final Map<String, Optional<Channel>> channelTable = new ConcurrentHashMap<>();

    private Bootstrap bootstrap;

    public ConnectionManager(Bootstrap bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Channel getChannel(String host, int port) {
        String key = host + ":" + port;
        // 从缓存中获取连接，如缓存中没有，则直接创建连接（注意：concurrentHashMap复合操作才是线程安全的）
        Optional<Channel> optionalChannel = channelTable.computeIfAbsent(key, k -> {
            try {
                ChannelFuture syncFuture = bootstrap.connect(host, port).sync();
                Channel channel = syncFuture.channel();
                // 连接关闭时，进行移除
                channel.closeFuture().addListener(future -> {channelTable.remove(key);});
                return Optional.of(channel);
            } catch (InterruptedException e) {
                log.error("建立连接失败：" + e.getMessage());
                return Optional.empty();
            }
        });
        // channel无效时清理
        if (optionalChannel.isEmpty() || !optionalChannel.get().isActive()) {
            channelTable.remove(key);
            return null;
        }

        return optionalChannel.get();
    }




}
