package com.example.myrpc.rpc.codec;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.example.myrpc.rpc.message.Message;
import com.example.myrpc.rpc.message.Response;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.nio.charset.StandardCharsets;

/**
 * 编码器：Response -> byte[]
 */
public class ResponseMessageEncoder extends MessageToByteEncoder<Response> {

    @Override
    protected void encode(ChannelHandlerContext ctx, Response Response, ByteBuf out) throws Exception {
        // length
        // magic
        // type
        // body
        byte[] magic = Message.MAGIC_BYTES;
        byte type = Message.MessageType.RESPONSE.code;
        byte[] body = serializeResponse(Response);
        int length = magic.length + Byte.BYTES + body.length;
        // 写入
        out.writeInt(length);
        out.writeBytes(magic);
        out.writeByte(type);
        out.writeBytes(body);
    }


    /**
     * 使用json序列化对象
     */
    public byte[] serializeResponse(Response Response) {
        String jsonString = JSONObject.toJSONString(Response);
        return jsonString.getBytes(StandardCharsets.UTF_8);
    }
}
