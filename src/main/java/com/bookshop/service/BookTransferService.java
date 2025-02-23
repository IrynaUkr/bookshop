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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Service
@AllArgsConstructor
@Slf4j
public class BookTransferService {

    private final UserRepository useRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;
    // Thread pool for processing book orders
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    // Lock map to manage locks per book
    private final ConcurrentHashMap<Integer, Lock> bookLocks = new ConcurrentHashMap<>();


    public void transferBooks(BookOrderRequest request) {
        Order parentOrder = new Order();
        User user = useRepository.getUserById(request.getUserId()).orElseThrow();
        parentOrder.setUser(user);
        parentOrder.setOrderDate(LocalDateTime.now());

        List<Item> orderItems = new ArrayList<>();

        try {
            for (Item item : request.getOrderItems()) {
                log.info("Order sent to execute service");
                executorService.execute(() -> processOrderItem(item, parentOrder, orderItems));
            }
            parentOrder.setItems(orderItems);
            orderRepository.save(parentOrder);
            log.info("order placed successfully with ID: {}", parentOrder.getId());
        } finally {
            for (Item item : request.getOrderItems()) {
                bookLocks.remove(item.getBookId());
            }
            // The ExecutorService will not shut down immediately, but it will no longer accept new tasks,
            // and once all threads have finished current tasks, the ExecutorService shuts down.
            // All tasks submitted to the ExecutorService before shutdown() is called, are executed.
            executorService.shutdown();
        }
    }

    private void processOrderItem(Item item, Order parentOrder, List<Item> orderItems) {
        // Lock per book, the atomic method make sure that only one thread at a time will be allowed to update
        Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());
        // Acquire the lock before processing the book
        lock.lock();
        try {
            log.info("{} starts with book: item id {} and quantity {} processing --- ",
                    Thread.currentThread().getName(), item.getBookId(), item.getQuantity());
            Book book = bookRepository.findById(item.getBookId())
                    .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));

            if (book.getStock() < item.getQuantity()) {
                throw new RuntimeException("Insufficient stock for book: " + book.getTitle());
            }
            log.info("{} reducing the stock quantity ", Thread.currentThread().getName());
            book.setStock(book.getStock() - item.getQuantity());
            bookRepository.save(book);
            Item orderItem = getItem(item, parentOrder, book);
            orderItems.add(orderItem);
        } finally {
            // Release the lock
            lock.unlock();
        }
    }

    private static Item getItem(Item item, Order parentOrder, Book book) {
        Item orderItem = new Item();
        orderItem.setOrder(parentOrder);
        orderItem.setBookId(book.getId());
        orderItem.setQuantity(item.getQuantity());
        return orderItem;
    }
}
