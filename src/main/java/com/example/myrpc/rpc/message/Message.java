package com.example.myrpc.rpc.message;

import lombok.Data;

import java.nio.charset.StandardCharsets;

/**
 * 自定义的消息协议
 * /**
 * * 协议格式 / Protocol Format:
 * +----------------+----------------+--------+--------------------------+
 * |  Total Length  |  Magic Number  |  Type  |      Message Content     |
 * |    (4 Bytes)   |    (N Bytes)   | (1 Byte)|       (N Bytes)         |
 * +----------------+----------------+--------+--------------------------+
 * |<--- Header ----------------------------->|<--- Body --------------->|
 */
@Data
public class Message {
    public static final byte[] MAGIC_BYTES = "myrpc".getBytes(StandardCharsets.UTF_8);
    // 消息长度（解决粘包，半包问题）
    private int totalLength;
    // 魔数
    private byte[] magic;
    // 消息类型（方便反序列化为指定对象）
    private MessageType messageType;
    // 消息内容
    private byte[] body;

//    public Message(byte[] magic, MessageType messageType, byte[] body) {
//        this.magic = magic;
//        this.messageType = messageType;
//        this.body = body;
//        // 消息长度
//        this.totalLength = this.magic.length  + Byte.BYTES + body.length;
//    }

    /**
     * 消息类型
     */
    public enum MessageType {
        REQUEST(1),
        RESPONSE( 2);

        public final byte code;

        MessageType(int code) {
            this.code = (byte)code;
        }
    }


}
