package com.bookshop.thread;

public class MyRunnable implements Runnable {
    @Override
    public void run() {

        System.out.println(Thread.currentThread().getName());
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10; i++) {
            MyRunnable myRunnable = new MyRunnable();
            Thread thread = new Thread(myRunnable);
            thread.start();
        }


    }
}
