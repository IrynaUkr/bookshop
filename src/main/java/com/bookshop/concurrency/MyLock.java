package com.bookshop.concurrency;

//naive implementation of the Lock class
public class MyLock {
    private boolean isLocked = false;

    public synchronized void lock()
            throws InterruptedException {
        while (isLocked) {
            wait();
        }
        isLocked = true;
    }

    public synchronized void unlock() {
        isLocked = false;
        notify();
    }
}

class Counter {

    MyLock lock = new MyLock();
    int instanceCounter = 0;

    public static void main(String[] args) throws InterruptedException {
        Counter counter = new Counter();
        for (int i = 0; i < 10; i++) {
            new Thread(() -> {
                try {
                    counter.increment();
                    System.out.println(Thread.currentThread().getName() + ":" + counter.instanceCounter);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }).start();
        }
        Thread.sleep(1000);
        System.out.println("counter in main = " + counter.instanceCounter + Thread.currentThread().getName());
    }

    public void increment() throws InterruptedException {
        lock.lock();
        Thread.sleep(100);
        ++instanceCounter;
        lock.unlock();
    }
}
