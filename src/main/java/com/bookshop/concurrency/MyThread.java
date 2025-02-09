package com.bookshop.concurrency;

public class MyThread extends Thread{
    @Override
    public void run() {
        System.out.println(this.getName()+ "is running");
        super.run();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            MyThread myThread = new MyThread();
            myThread.start();
        }
        System.out.println("" +
                "main thread is running");
    }
}
