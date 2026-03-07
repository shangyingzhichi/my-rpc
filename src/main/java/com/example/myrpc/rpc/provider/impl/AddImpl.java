package com.example.myrpc.rpc.provider.impl;

import com.example.myrpc.rpc.api.IAdd;

/**
 * 真正的实现功能的类，provider会再包装一层
 */
public class AddImpl implements IAdd{

    @Override
    public int add(int a, int b) {
        return a + b;
    }
}
