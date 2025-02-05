package com.bookshop.bookTransferService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.model.Book;
import com.bookshop.model.Order;
import com.bookshop.model.Item;
import com.bookshop.model.User;
import com.bookshop.repository.BookRepository;
import com.bookshop.repository.OrderRepository;
import com.bookshop.repository.UserRepository;
import lombok.AllArgsConstructor;


@Service
@AllArgsConstructor
public class BookTransferService {
    // Lock map to manage locks per book
    private final ConcurrentHashMap<Integer, Lock> bookLocks = new ConcurrentHashMap<>();
    private final UserRepository useRepository;
    private final BookRepository bookRepository;
    private final OrderRepository orderRepository;

    public void transferBooks(BookOrderRequest request) {
        Order parentOrder = new Order();
        User user = useRepository.getUserById(request.getUserId()).orElseThrow();
        parentOrder.setUser(user);
        parentOrder.setOrderDate(LocalDateTime.now());

        List<Item> orderItems = new ArrayList<>();

        try {
            for (Item item : request.getOrderItems()) {
                // Lock per book
                Lock lock = bookLocks.computeIfAbsent(item.getBookId(), id -> new ReentrantLock());

                // Acquire the lock before processing the book
                lock.lock();
                try {
                    Book book = bookRepository.findById(item.getBookId())
                            .orElseThrow(() -> new RuntimeException("Book not found: " + item.getBookId()));

                    if (book.getStock() < item.getQuantity()) {
                        throw new RuntimeException("Insufficient stock for book: " + book.getTitle());
                    }
                    // reduce stock amount
                    book.setStock(book.getStock() - item.getQuantity());
                    bookRepository.save(book);

                    // Create an order item for this book
                    Item orderItem = new Item();
                    orderItem.setOrder(parentOrder);
                    orderItem.setBookId(book.getId());
                    orderItem.setQuantity(item.getQuantity());

                    orderItems.add(orderItem);
                } finally {
                    // Release the lock
                    lock.unlock();
                }
            }

            // Save the parent order (and cascade to order items)
            parentOrder.setItems(orderItems);
            orderRepository.save(parentOrder);
            System.out.println("order placed successfully with ID: " + parentOrder.getId());
        } finally {

            for (Item item : request.getOrderItems()) {
                bookLocks.remove(item.getBookId());
            }
        }
    }
}
