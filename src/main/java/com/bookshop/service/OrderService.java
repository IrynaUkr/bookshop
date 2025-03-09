package com.bookshop.service;

import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
import com.bookshop.mapper.OrderDtoMapper;
import com.bookshop.model.Book;
import com.bookshop.model.Item;
import com.bookshop.model.Order;
import com.bookshop.model.User;
import com.bookshop.repository.BookRepository;
import com.bookshop.repository.OrderRepository;
import com.bookshop.repository.UserRepository;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Service
@AllArgsConstructor
@Slf4j
public class OrderService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);
    private final ConcurrentHashMap<Long, Lock> bookLocks = new ConcurrentHashMap<>();
    private final OrderDtoMapper orderDtoMapper;

    private static CompletableFuture<List<Item>> getListCompletableFuture(List<CompletableFuture<Item>> orderItemFutures) {
        return CompletableFuture.allOf(orderItemFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> orderItemFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    log.error("Order processing exception: {}", ex.getMessage());
                    throw new RuntimeException("Order processing error : " + ex.getMessage());
                });
    }

    public CompletableFuture<OrderDto> createOrder(BookOrderRequest bookOrderRequest) {
        List<CompletableFuture<Item>> orderItemFutures = normaliseOrderedItems(bookOrderRequest.getOrderItems())
                .stream()
                .map(item -> supplyAsync(() -> processBookItem(item), executorService))
                .toList();

        CompletableFuture<List<Item>> allOrderItemsFuture = getListCompletableFuture(orderItemFutures);

        return allOrderItemsFuture.thenApply(orderItems -> {
            Order order = createNewOrder(bookOrderRequest, orderItems);
            orderRepository.save(order);
            log.info("order was processed {} with thread: {}", order, Thread.currentThread().getName());
            return orderDtoMapper.mapOrderDto(order);
        }).orTimeout(10, TimeUnit.SECONDS);
    }

    private Order createNewOrder(BookOrderRequest bookOrderRequest, List<Item> orderItems) {
        Order order = new Order();
        User user = userRepository.findById(bookOrderRequest.getUserId()).orElseThrow();
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

    public OrderDto getOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        return orderDtoMapper.mapOrderDto(order);
    }

    public void deleteOrder(Long orderId) {
        orderRepository.deleteById(orderId);
    }

    public CompletableFuture<OrderDto> updateOrder(Long orderId, BookOrderRequest bookOrderRequest) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("order not found"));
        log.info(" order to update: {}", order);
        List<Item> existingItems = order.getItems();
        List<Item> orderedItems = normaliseOrderedItems(bookOrderRequest.getOrderItems());
        List<Item> updatedExistingItems = deleteNotOrderedItems(existingItems, orderedItems);

        List<CompletableFuture<Item>> orderItemFutures = orderedItems
                .stream()
                .map(item -> supplyAsync(() -> processUpdateItem(item, updatedExistingItems), executorService))
                .toList();

        CompletableFuture<List<Item>> allOrderItemsFuture = getListCompletableFuture(orderItemFutures);

        return allOrderItemsFuture.thenApply(orderItems -> {
            orderRepository.delete(order);
            Order orderUpdated = createNewOrder(bookOrderRequest, orderItems);
            orderRepository.save(order);
            log.info("order was updated {} by thread: {}", orderUpdated, Thread.currentThread().getName());
            return orderDtoMapper.mapOrderDto(orderUpdated);
        }).orTimeout(10, TimeUnit.SECONDS);
    }

    private List<Item> deleteNotOrderedItems(List<Item> existingItems, List<Item> orderedItems) {
        log.info("remove not ordered items---->");
        List<Item> updatedList = new ArrayList<>();
        HashSet<Item> orderedItemSet = new HashSet<>(orderedItems);
        for (Item item : existingItems) {
            if (orderedItemSet.contains(item)) {
                updatedList.add(item);
            } else {
                Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());
                log.info("update book for removal------->{}", item.getBookId());
                lock.lock();
                try {
                    Book book = bookRepository.findById(item.getBookId()).orElseThrow();
                    book.setStock(book.getStock() + item.getQuantity());
                    bookRepository.save(book);
                    log.info("Removed item: {} - Stock restored for book {}", item, book.getTitle());
                } finally {
                    lock.unlock();
                    bookLocks.remove(item.getBookId());
                }
            }
        }
        return updatedList;
    }

    private Item processUpdateItem(Item requestedItem, List<Item> existingItems) {
        Lock lock = bookLocks.computeIfAbsent(requestedItem.getBookId(), id -> new ReentrantLock());
        log.info("update started with thread: {} for book id:{} and NEW quantity {}]",
                Thread.currentThread().getName(), requestedItem.getBookId(), requestedItem.getQuantity());
        lock.lock();
        try {
            Item existingItem = getExistingItemWithRequestedItemId(requestedItem, existingItems);
            log.info("the existing item {} ", existingItem);
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
        int stockAdjustment = requestedItem.getQuantity() - existingItem.getQuantity();
        Lock lock = bookLocks.computeIfAbsent(requestedItem.getBookId(), id -> new ReentrantLock());
        log.info("adjustBookStock started with thread: {} for book id:{} and NEW quantity {}]",
                Thread.currentThread().getName(), requestedItem.getBookId(), requestedItem.getQuantity());
        lock.lock();
        try {
        Book book = bookRepository.findById(existingItem.getBookId()).orElseThrow();
        if (book.getStock() + existingItem.getQuantity() < requestedItem.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle()
                    + "requested" + requestedItem.getQuantity()
                    + "in stock" + book.getStock());
        }
            book.setStock(book.getStock() - stockAdjustment);
        bookRepository.save(book);
            log.info("Stock updated: Book ID: {}, Old Stock: {}, New Stock: {}, Processed by: {}",
                    book.getId(), requestedItem.getQuantity(), book.getStock(), Thread.currentThread().getName());
            return requestedItem;
        } finally {
            lock.unlock();
        }
    }

    private Item getExistingItemWithRequestedItemId(Item requestedItem, List<Item> existingItems) {
        return existingItems.stream()
                .filter(item -> Objects.equals(item.getBookId(), requestedItem.getBookId()))
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
