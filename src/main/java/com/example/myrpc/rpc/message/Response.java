package com.example.myrpc.rpc.message;

import lombok.Data;

@Data
public class Response {
    public static final int SUCCESS = 200;
    public static final int ERROR = 400;

    private Object result;
    private int code;
    private String msg;

    public static Response success(Object result) {
        Response response = new Response();
        response.setResult(result);
        response.setCode(SUCCESS);
        return response;
    }

    public static Response fail(int code, String msg) {
        Response response = new Response();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }

    public static Boolean isSuccess(Response response) {
        return response.getCode() == SUCCESS;
    }
}
