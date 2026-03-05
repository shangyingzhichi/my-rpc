package com.example.myrpc.rpc.codec;

import com.alibaba.fastjson2.JSONObject;
import com.example.myrpc.rpc.message.Message;
import com.example.myrpc.rpc.message.Request;
import com.example.myrpc.rpc.message.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * 解码器：数据帧 -> Message对象
 */
public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    public MessageDecoder() {
        /**
         * 最大长度
         * length起始位置
         * length长度
         * 修正
         * 跳过（长度字段）
         */
        super(1024 * 1024, 0, Integer.BYTES, 0, Integer.BYTES);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        // 一帧数据
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        // frame -> Message(Request/Response)
        byte[] magic = new byte[Message.MAGIC_BYTES.length];
        frame.readBytes(magic);
        if (!Arrays.equals(magic, Message.MAGIC_BYTES)) {
            throw new RuntimeException("Invalid magic bytes");
        }
        byte messageType = frame.readByte();
        byte[] body = new byte[frame.readableBytes()];
        frame.readBytes(body);
        // 反序列化消息内容
        if(Objects.equals(Message.MessageType.REQUEST.code, messageType)){
            return deSerializeRequest(body);
        } else if (Objects.equals(Message.MessageType.RESPONSE.code, messageType)) {
            return deSerializeResponse(body);
        } else {
            throw new RuntimeException("Invalid message type" + messageType);
        }


    }

    public Request deSerializeRequest(byte[] body) {
        String json = new String(body, StandardCharsets.UTF_8);
        return JSONObject.parseObject(json, Request.class);
    }

    public Response deSerializeResponse(byte[] body) {
        String json = new String(body, StandardCharsets.UTF_8);
        return JSONObject.parseObject(json, Response.class);
    }


}
