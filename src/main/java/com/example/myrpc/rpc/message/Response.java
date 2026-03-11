package com.example.myrpc.rpc.message;

import lombok.Data;

@Data
public class Response {
    public static final int SUCCESS = 200;
    public static final int ERROR = 400;

    private int requestId;
    private Object result;
    private int code;
    private String msg;

    public static Response success(int requestId, Object result) {
        Response response = new Response();
        response.setRequestId(requestId);
        response.setResult(result);
        response.setCode(SUCCESS);
        return response;
    }

    public static Response fail(int requestId, int code, String msg) {
        Response response = new Response();
        response.setRequestId(requestId);
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }

    public static Boolean isSuccess(Response response) {
        return response.getCode() == SUCCESS;
    }
}
