package com.bookshop.service;

import com.bookshop.dto.BookOrderRequest;
import com.bookshop.model.Book;
import com.bookshop.model.Item;
import com.bookshop.model.Order;
import com.bookshop.model.User;
import com.bookshop.repository.BookRepository;
import com.bookshop.repository.ItemRepository;
import com.bookshop.repository.OrderRepository;
import com.bookshop.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
@AllArgsConstructor
public class BookTransferServiceFuture {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Integer, Lock> bookLocks = new ConcurrentHashMap<>();

    public void transferBooks(BookOrderRequest request) {
        Order parentOrder = new Order();
        User user = userRepository.getUserById(request.getUserId()).orElseThrow();
        parentOrder.setUser(user);
        parentOrder.setOrderDate(LocalDateTime.now());

        List<Item> orderItems = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(request.getOrderItems().size());

        List<Future<?>> futures = new ArrayList<>();

        try {
            for (Item item : request.getOrderItems()) {
                Future<?> future = executorService.submit(() -> {
                    try {
                        processBookOrder(item, parentOrder, orderItems);
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }

            // Wait for all tasks to complete with a timeout
            if (!latch.await(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Order processing timed out");
            }

            // Check for any exceptions in the futures
            for (Future<?> future : futures) {
                try {
                    // This will throw an exception if the task failed,returns null if the task has finished correctly.
                    future.get();
                } catch (ExecutionException e) {
                    throw new RuntimeException("Error processing order: " + e.getCause().getMessage());
                }
            }
            parentOrder.setItems(new ArrayList<>(orderItems));
            orderRepository.save(parentOrder);
            log.info("Order placed successfully with ID: {}", parentOrder.getId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Order processing interrupted", e);
        } finally {
            for (Item item : request.getOrderItems()) {
                bookLocks.remove(item.getBookId());
            }
        }
    }

    private void processBookOrder(Item item, Order parentOrder, List<Item> orderItems) {
        Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());
        log.info("future service : processing started with thread: {} for book id:{}",
                Thread.currentThread().getName(), item.getBookId());
        lock.lock();
        try {
            Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));
            if (book.getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for book: " + book.getTitle());
            }
            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);
            log.info("future service : the quantity changed with thread: {}", Thread.currentThread().getName());
            Item orderItem = new Item();
            orderItem.setOrder(parentOrder);
            orderItem.setBookId(book.getId());
            orderItem.setQuantity(item.getQuantity());
            orderItems.add(orderItem);


        } finally {
            lock.unlock();
        }
    }

    // instead of  calling shut down in main tread, when the particular order is complete,
    // we can do it when the app Book transfer service bean is going to destroy
    @PreDestroy
    public void shutdown() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}