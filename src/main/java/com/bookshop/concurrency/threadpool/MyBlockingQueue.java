package com.bookshop.concurrency.threadpool;

import java.util.LinkedList;
import java.util.List;

public class MyBlockingQueue {
    private final List queue = new LinkedList();
    private int  limit = 10;

    public MyBlockingQueue(int limit){
        this.limit = limit;
    }


    public synchronized void enqueue(Object item)
            throws InterruptedException  {
        while(this.queue.size() == this.limit) {
            wait();
        }
        this.queue.add(item);
        // in case we added first item into an empty list, we need to notify all threads that was waiting to consume element
        if(this.queue.size() == 1) {
            notifyAll();
        }
    }


    public synchronized Object dequeue()
            throws InterruptedException{
        while(this.queue.size() == 0){
            wait();
        }
        // in case we reached the limit of the queue, and we are about to remove one element,
        // so addition elements can be unblocked now:
        // we want to notify all threads that are waiting to add elements
        if(this.queue.size() == this.limit){
            notifyAll();
        }

        return this.queue.remove(0);
    }

}
