package com.bookshop.service;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.model.Book;
import com.bookshop.model.Item;
import com.bookshop.model.Order;
import com.bookshop.model.User;
import com.bookshop.repository.BookRepository;
import com.bookshop.repository.OrderRepository;
import com.bookshop.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@AllArgsConstructor
@Slf4j
public class OrderServiceCompletableFuture {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Integer, Lock> bookLocks = new ConcurrentHashMap<>();

    public CompletableFuture<Order> createOrder(BookOrderRequest bookOrderRequest) {

        List<CompletableFuture<Item>> orderItemFutures = bookOrderRequest.getOrderItems()
                .stream()
                .map(item -> supplyAsync(processBookItem(item), executorService))
                .toList();

        // Combine all book processing tasks
        CompletableFuture<List<Item>> allOrderItemsFuture =
                CompletableFuture.allOf(orderItemFutures.toArray(new CompletableFuture[0]))
                        .thenApply(v -> orderItemFutures.stream()
                                .map(CompletableFuture::join)
                                .collect(Collectors.toList()));

        // Once all order items are processed, create the order

        return allOrderItemsFuture.thenApply(orderItems -> {
            Order order = new Order();
            User user = userRepository.getUserById(bookOrderRequest.getUserId()).orElseThrow();
            order.setUser(user);
            order.setOrderDate(LocalDateTime.now());
            order.setItems(orderItems);
            orderRepository.save(order);
            log.info("order was processed {} with thread: {}", order, Thread.currentThread().getName());
            return order;
        });
    }

    private Supplier<Item> processBookItem(Item item) {
        //processing ordered book items concurrently, make sure that unique book process synchronized
        Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());
        log.info(" processing started with thread: {} for book id:{}",
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
            log.info("the book quantity {} changed with thread: {}", book, Thread.currentThread().getName());
            item.setQuantity(item.getQuantity());
            log.info("item was processed {} with thread: {}", item, Thread.currentThread().getName());
        } finally {
            lock.unlock();
        }
        return () -> item;
    }
}
