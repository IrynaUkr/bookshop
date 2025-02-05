package com.bookshop.thread;

public class Account {
    public int balance;

    public Account(int balance) {
        this.balance = balance;
    }

    public static void main(String[] args) throws InterruptedException {
        Account account = new Account(100000000);
        Thread depositThread = new Thread(() -> {
            for (int i = 0; i < 10_000_000; i++) {
                account.deposit(10);
            }
        });
        Thread withdrowThread = new Thread(() -> {
            for (int i = 0; i < 10_000_000; i++) {
                account.withdraw(10);
            }
        });
        depositThread.start();
        withdrowThread.start();
        depositThread.join();
        withdrowThread.join();
        System.out.println(account.balance);
    }

    public void deposit(int amount) {
        checkAmountNotNegative(amount);
        balance += amount;
    }

    public void withdraw(int amount) {
        checkAmountNotNegative(amount);
        if (amount < balance) {
            balance -= amount;
        } else {
            throw new IllegalArgumentException("not enough money");
        }
    }

    public void checkAmountNotNegative(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}
