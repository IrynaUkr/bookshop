package com.bookshop.service;

import static com.bookshop.controller.BookOrderController.handleError;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.bookshop.repository.ItemRepository;
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
    private final ItemRepository itemRepository;

    private static List<Item> handleException(Throwable ex) {
        handleError(ex);
        log.error("Order processing exception: {}", ex.getMessage());
        throw new RuntimeException("Order processing error: " + ex.getMessage());
    }

    private static CompletableFuture<List<Item>> getListCompletableFuture(List<CompletableFuture<Item>> orderItemFutures) {
        return CompletableFuture.allOf(orderItemFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> orderItemFutures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()))
                .orTimeout(5, TimeUnit.SECONDS)
                .exceptionally(OrderService::handleException);
    }

    @Transactional
    public OrderDto createOrder(BookOrderRequest bookOrderRequest) throws ExecutionException, InterruptedException {
        List<CompletableFuture<Item>> orderItemFutures = normaliseOrderedItems(bookOrderRequest.getOrderItems())
                .stream()
                .map(item -> supplyAsync(() -> processBookItem(item), executorService)).toList();

        CompletableFuture<List<Item>> allOrderItemsFuture = getListCompletableFuture(orderItemFutures);

        return allOrderItemsFuture.thenApply(orderItems -> {
            Order order = createNewOrder(bookOrderRequest, orderItems);
            orderRepository.save(order);
            return orderDtoMapper.mapOrderDto(order);
        }).orTimeout(10, TimeUnit.SECONDS).get();
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

    @Transactional
    public OrderDto updateOrder(Long orderId, BookOrderRequest bookOrderRequest) throws ExecutionException, InterruptedException {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found"));
        log.info("Order to update: {}", order);

        List<Item> existingItems = new ArrayList<>(order.getItems());
        List<Item> newItems = normaliseOrderedItems(bookOrderRequest.getOrderItems());

        restoreStockForRemovedItems(existingItems, newItems);
        List<CompletableFuture<Item>> orderItemFutures = newItems.stream()
                .map(item -> supplyAsync(() -> processUpdateItem(item, existingItems), executorService))
                .toList();

        CompletableFuture<List<Item>> allOrderItemsFuture = getListCompletableFuture(orderItemFutures);

        return allOrderItemsFuture.thenApply(orderItems -> {
            for (Item item : orderItems) {
                item.setOrder(order);
            }
            order.setItems(orderItems);
            log.info("Order was updated: {}", order);
            return orderDtoMapper.mapOrderDto(order);
        }).orTimeout(10, TimeUnit.SECONDS).get();
    }

    private List<Item> normaliseOrderedItems(List<Item> items) {
        return new ArrayList<>(items.stream()
                .collect(Collectors.toMap(Item::getBookId, item -> item, (existing, newItem) -> {
                    existing.setQuantity(existing.getQuantity() + newItem.getQuantity());
                    return existing;
                })).values());
    }

    private Item processUpdateItem(Item requestedItem, List<Item> existingItems) {
        Lock lock = bookLocks.computeIfAbsent(requestedItem.getBookId(), id -> new ReentrantLock());
        lock.lock();
        try {
            Item existingItem = existingItems.stream()
                    .filter(item -> Objects.equals(item.getBookId(), requestedItem.getBookId()))
                    .findFirst().orElse(null);

            return existingItem == null ? updateBookStock(requestedItem) : adjustBookStock(requestedItem, existingItem);
        } finally {
            lock.unlock();
        }
    }

    private void restoreStockForRemovedItems(List<Item> existingItems, List<Item> newItems) {
        HashMap<Long, Item> newItemsMap = new HashMap<>();
        for (Item newItem : newItems) {
            newItemsMap.put(newItem.getBookId(), newItem);
        }

        existingItems.stream().filter(item -> !newItemsMap.containsKey(item.getBookId())).forEach(this::restoreStockForRemovedItem);
    }

    private void restoreStockForRemovedItem(Item item) {
        Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());
        lock.lock();
        try {
            Book book = bookRepository.findById(item.getBookId()).orElseThrow();
            book.setStock(book.getStock() + item.getQuantity());
            // item Removed  from the order and deleted  from the database
            item.getOrder().getItems().remove(item);
            item.setOrder(null);
            itemRepository.delete(item);
            bookRepository.save(book);
            log.info("Stock restored for removed item: {}", item);
        } finally {
            lock.unlock();
            bookLocks.remove(item.getBookId());
        }
    }

    private Item adjustBookStock(Item requestedItem, Item existingItem) {
        int stockAdjustment = requestedItem.getQuantity() - existingItem.getQuantity();
        Book book = bookRepository.findById(existingItem.getBookId()).orElseThrow();
        if (book.getStock() + existingItem.getQuantity() < requestedItem.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle());
        }
        existingItem.setQuantity(requestedItem.getQuantity());
        book.setStock(book.getStock() - stockAdjustment);
        bookRepository.save(book);
        return existingItem;
    }

    private Item updateBookStock(Item item) {
        Book book = bookRepository.findById(item.getBookId()).orElseThrow(() -> new ProductNotFoundException("Book with requested id not found"));
        if (book.getStock() < item.getQuantity()) {
            throw new InsufficientStockException("Insufficient stock for book: " + book.getTitle());
        }
        book.setStock(book.getStock() - item.getQuantity());
        bookRepository.save(book);
        return item;
    }

    public OrderDto getOrder(Long orderId) {
        return orderDtoMapper.mapOrderDto(orderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new OrderNotFoundException("Order not found")));
    }

    @Transactional
    public void deleteOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new OrderNotFoundException("Order not found"));
        order.getItems()
                .forEach(this::restoreStockForRemovedItem);

        orderRepository.deleteById(orderId);
    }



    private Order createNewOrder(BookOrderRequest bookOrderRequest, List<Item> orderItems) {
        User user = userRepository.findById(bookOrderRequest.getUserId()).orElseThrow();
        Order order = Order.builder()
                .user(user)
                .orderDate(LocalDateTime.now())
                .items(orderItems)
                .build();
        orderItems.forEach(item -> item.setOrder(order));
        return order;
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
