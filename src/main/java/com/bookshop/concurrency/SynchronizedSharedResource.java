package com.bookshop.concurrency;

import com.bookshop.model.Book;
import com.bookshop.model.User;

public class SynchronizedSharedResource {

    public static void classMethod(String input) {
        synchronized (Book.class) {
            System.out.println("synchronised static block, monitor Book class executing simultaneously..."
                    + input + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println("synchronised static block, monitor Book finished" +
                    input + Thread.currentThread().getName());
        }

        synchronized (User.class) {
            System.out.println("synchronised static block, monitor User class executing simultaneously" +
                    input + Thread.currentThread().getName());
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
            System.out.println("synchronised static block, monitor User finished" +
                    input + Thread.currentThread().getName());
        }
    }

    public static void main(String[] args) {
        SynchronizedSharedResource sharedResource = new SynchronizedSharedResource();
        Thread thread1 = new Thread(() -> classMethod("1111"));
        Thread thread2 = new Thread(() -> classMethod("22222"));
        Thread thread3 = new Thread(() -> sharedResource.methodA());
        Thread thread4 = new Thread(() -> sharedResource.methodB());
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
    }

    public void methodA() {
        synchronized (this) {  // Locking on 'this'
            System.out.println("____block A executing instance method got the Monitor..."
                    + Thread.currentThread().getName());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            System.out.println("Method A finished.Monitor released");
        }
    }

    public void methodB() {
        synchronized (this) {  // Also locking on 'this'
            System.out.println("___block b executing in synch instance method.Allowed to get Monitor.."
                    + Thread.currentThread().getName());
        }
    }
}
