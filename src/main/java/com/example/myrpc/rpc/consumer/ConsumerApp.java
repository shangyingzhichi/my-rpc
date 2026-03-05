package com.example.myrpc.rpc.consumer;

public class ConsumerApp {

    public static void main(String[] args) throws InterruptedException {
        Consumer consumer = new Consumer();
        consumer.add(1, 2);
        consumer.add(2, 3);
    }
}
