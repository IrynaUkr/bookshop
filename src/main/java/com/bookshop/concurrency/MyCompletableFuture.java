package com.bookshop.concurrency;

import java.util.concurrent.CompletableFuture;
public class MyCompletableFuture {

    public static void printInput(String input) {
        CompletableFuture.runAsync(() -> System.out.println(input));
    }

    public static Integer compute(int input) {
        System.out.println("compute called");
        if (input < 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            throw new IllegalArgumentException("input is negative");
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(Thread.currentThread().getName());

        System.out.println("compute finished");
        return input * 2;
    }

    public static CompletableFuture<Integer> computeGetResult(int num) {

        return CompletableFuture.supplyAsync(() -> compute(num));
    }

    public static void main(String[] args) {
        computeGetResult(-7)
                .thenApply(data -> {
                    System.out.println(Thread.currentThread().getName());
                    return data + 1;
                })
                .exceptionally(err -> {
                    System.out.println("exception caught: " + err.getMessage());
                    return 10;
                })
                .thenAccept(System.out::println)
                .thenRun(() -> System.out.println(Thread.currentThread().getName()));

        System.out.println("started computation in " + Thread.currentThread().getName());
        System.out.println("combine two completable futures in " + Thread.currentThread().getName());
        CompletableFuture<Integer> cf1 = computeGetResult(3);
        CompletableFuture<Integer> cf2 = computeGetResult(5);
        cf1.thenCombine(cf2, (a, b) -> a + b)
                .thenAccept(System.out::println);
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("finished computation in " + Thread.currentThread().getName());
    }
}
