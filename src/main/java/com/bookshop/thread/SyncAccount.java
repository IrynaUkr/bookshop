package com.bookshop.thread;

public class SyncAccount {
    public int balance;

    public SyncAccount(int balance) {
        this.balance = balance;
    }

    public static void main(String[] args) throws InterruptedException {
        SyncAccount account = new SyncAccount(10_000_000);
        Thread depositThread = new Thread(() -> {
            for (int i = 0; i < 10_000_000; i++) {
                account.deposit(10);
            }
        });
        Thread withdrowThread = new Thread(() -> {
            for (int i = 0; i < 10_000_000; i++) {
                try {
                    account.withdrowWhenEnoughMoney(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        depositThread.start();
        withdrowThread.start();
        depositThread.join();
        withdrowThread.join();
        System.out.println(account.balance);
    }

    public synchronized void deposit(int amount) {
        checkAmountNotNegative(amount);
        balance += amount;
        notifyAll();
    }

    public void withdraw(int amount) {
        checkAmountNotNegative(amount);
        if (amount < balance) {
            balance -= amount;
        } else {
            throw new IllegalArgumentException("not enough money");
        }
    }

    public synchronized void withdrowWhenEnoughMoney(int amount) throws InterruptedException {
        checkAmountNotNegative(amount);
        while (balance < amount) {
            wait();
        }
        balance -= amount;
    }

    public void checkAmountNotNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
