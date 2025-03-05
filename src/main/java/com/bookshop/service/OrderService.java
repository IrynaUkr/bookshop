package com.bookshop.service;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.dto.OrderDto;
import com.bookshop.exception.InsufficientStockException;
import com.bookshop.exception.OrderNotFoundException;
import com.bookshop.exception.ProductNotFoundException;
import com.bookshop.mapper.OrderDtoMapper;
import com.bookshop.model.Book;
import com.bookshop.model.Item;
import com.bookshop.model.Order;
import com.bookshop.model.User;
import com.bookshop.repository.BookRepository;
import com.bookshop.repository.OrderRepository;
import com.bookshop.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Integer, Lock> bookLocks = new ConcurrentHashMap<>();
    private final OrderDtoMapper orderDtoMapper;

    private static CompletableFuture<List<Item>> getListCompletableFuture(List<CompletableFuture<Item>> orderItemFutures) {
        return CompletableFuture.allOf(orderItemFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> orderItemFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Order processing timed out: {}", ex.getMessage());
                    throw new RuntimeException("Order processing error : " + ex.getMessage());
                });
    }

    public CompletableFuture<OrderDto> createOrder(BookOrderRequest bookOrderRequest) {

        List<CompletableFuture<Item>> orderItemFutures = normaliseOrderedItems(bookOrderRequest.getOrderItems())
                .stream()
                ///synchronized processing
                .map(item -> supplyAsync(() -> processBookItem(item), executorService))
                .toList();

        CompletableFuture<List<Item>> allOrderItemsFuture = getListCompletableFuture(orderItemFutures);

        return allOrderItemsFuture.thenApply(orderItems -> {
            Order order = getOrderById(bookOrderRequest, orderItems);
            orderRepository.save(order);
            log.info("order was processed {} with thread: {}", order, Thread.currentThread().getName());
            return orderDtoMapper.mapOrderDto(order);
        }).orTimeout(10, TimeUnit.SECONDS);
    }

    private Order getOrderById(BookOrderRequest bookOrderRequest, List<Item> orderItems) {
        Order order = new Order();
        User user = userRepository.getUserById(bookOrderRequest.getUserId()).orElseThrow();
        order.setUser(user);
        order.setOrderDate(LocalDateTime.now());
        order.setItems(orderItems);
        for (Item item : orderItems) {
            item.setOrder(order);
        }
        return order;
    }

    private Item processBookItem(Item item) {
        Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());
        log.info(" processing started with thread: {} for book id:{}",
                Thread.currentThread().getName(), item.getBookId());
        lock.lock();
        try {
            updateBookStock(item);
            log.info("the book stock has been changed with thread: {}", Thread.currentThread().getName());
        } finally {
            lock.unlock();
            bookLocks.remove(item.getBookId());
        }
        return item;
    }

    private Item updateBookStock(Item item) {
        Book book = bookRepository.findById(item.getBookId())
                .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));
        if (book.getStock() < item.getQuantity()) {
            throw new RuntimeException("Insufficient stock for book: " + book.getTitle());
        }
        book.setStock(book.getStock() - item.getQuantity());
        bookRepository.save(book);
        return item;
    }

    public OrderDto getOrderById(Integer orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        return orderDtoMapper.mapOrderDto(order);
    }

    public void deleteOrder(Integer orderId) {
        orderRepository.deleteById(orderId);
    }

    public CompletableFuture<OrderDto> updateOrder(Integer orderId, BookOrderRequest bookOrderRequest) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("order not found"));
        List<Item> existingItems = order.getItems();
        List<CompletableFuture<Item>> orderItemFutures = normaliseOrderedItems(bookOrderRequest.getOrderItems())
                .stream()
                .map(item -> supplyAsync(() -> processUpdateItem(item, existingItems), executorService))
                .toList();

        CompletableFuture<List<Item>> allOrderItemsFuture = getListCompletableFuture(orderItemFutures);

        return allOrderItemsFuture.thenApply(orderItems -> {
            order.setOrderDate(LocalDateTime.now());
            for (Item item : orderItems) {
                item.setOrder(order);
            }
            order.setItems(orderItems);
            orderRepository.save(order);
            log.info("order was updated {} by thread: {}", order, Thread.currentThread().getName());
            return orderDtoMapper.mapOrderDto(order);
        }).orTimeout(10, TimeUnit.SECONDS);
    }

    private Item processUpdateItem(Item requestedItem, List<Item> existingItems) {
        Lock lock = bookLocks.computeIfAbsent(requestedItem.getBookId(), id -> new ReentrantLock());
        log.info("update started with thread: {} for book id:{}",
                Thread.currentThread().getName(), requestedItem.getBookId());
        lock.lock();
        try {
            Item existingItem = getExistingItemWithRequestedItemId(requestedItem, existingItems);
            if (existingItem == null) {
                return updateBookStock(requestedItem);
            } else {
                return adjustBookStock(requestedItem, existingItem);
            }
        } finally {
            lock.unlock();
        }
    }

    private Item adjustBookStock(Item requestedItem, Item existingItem) {
        int stockAdjustment = existingItem.getQuantity() - requestedItem.getQuantity();
        Book book = bookRepository.findById(existingItem.getBookId()).orElseThrow();
        if (book.getStock() + existingItem.getQuantity() < requestedItem.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle()
                    + "requested" + requestedItem.getQuantity()
                    + "in old  order" + existingItem.getQuantity()
                    + "in stock" + book.getStock());
        }
        book.setStock(book.getStock() + stockAdjustment);
        bookRepository.save(book);
        existingItem.setQuantity(requestedItem.getQuantity());
        log.info("Stock updated: Book ID: {}, New Stock: {}, Processed by: {}",
                book.getId(), book.getStock(), Thread.currentThread().getName());
        return existingItem;
    }

    private Item getExistingItemWithRequestedItemId(Item requestedItem, List<Item> existingItems) {
        return existingItems
                .stream()
                .filter(item -> item.getId() == requestedItem.getId())
                .findFirst()
                .orElse(null);
    }

    private List<Item> normaliseOrderedItems(List<Item> items) {
        return new ArrayList<>(items.stream()
                .collect(Collectors.toMap(Item::getBookId,
                        item -> item,
                        (existing, newItem) -> {
                            existing.setQuantity(existing.getQuantity() + newItem.getQuantity());
                            return existing;
                        }))
                .values());
    }

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
