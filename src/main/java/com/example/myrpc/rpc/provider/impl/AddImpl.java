package com.example.myrpc.rpc.provider.impl;

import com.example.myrpc.rpc.api.IAdd;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * 真正的实现功能的类，provider会再包装一层
 */
public class AddImpl implements IAdd{

    @Override
    public int add(int a, int b) {
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        return a + b;
    }
}
