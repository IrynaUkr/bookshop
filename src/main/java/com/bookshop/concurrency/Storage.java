package com.bookshop.concurrency;


public class Storage {
    private int amount;

    public Storage(int amount) {
        this.amount = amount;
    }

    public static void main(String[] args) {
        Storage storage = new Storage(1);

        for (int i = 0; i < 10; i++) {
            new Thread(storage::decreaseAmount).start();
        }

        for (int i = 0; i < 20; i++) {
            new Thread(storage::increaseAmount).start();
        }

    }

    synchronized public void increaseAmount() {
        this.amount++;
        System.out.println(Thread.currentThread().getName() + " increased amount to " + this.amount);
        notifyAll();
    }

    synchronized public void decreaseAmount() {
        System.out.println(" start decreasing checking amount to " + this.amount);
        if (this.amount > 2) {
            this.amount= amount -2;
            System.out.println(Thread.currentThread().getName() + " decreased amount to " + this.amount);
        } else {
            System.out.println(Thread.currentThread().getName() + " not enough amount to decrease");
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
