package com.bookshop.service;

import com.bookshop.dto.BookOrderRequest;
import com.bookshop.model.Book;
import com.bookshop.model.Item;
import com.bookshop.repository.BookRepository;
import jakarta.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class RecommendationService {

    private final BookRepository bookRepository;
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public String getRecommendations(BookOrderRequest request) throws InterruptedException {
        List<Item> items = request.getOrderItems().stream().distinct().toList();
        List<Future> futureList = new ArrayList<>();
        List<Book> recommendations = new ArrayList<>();
        CountDownLatch countDownLatch = new CountDownLatch(items.size());

        for (Item item : items) {
            Book book = bookRepository.findById(item.getBookId()).get();
            Future<List<Book>> future = executorService.submit(() -> getRecommendations(book));
            countDownLatch.countDown();
            futureList.add(future);
        }
        // Wait for all tasks to complete with a timeout
        if (!countDownLatch.await(5, TimeUnit.SECONDS)) {
            log.info("not all recommendations were processed");
        }

        for (Future<?> future : futureList) {
            try {
                List<Book> list = (List<Book>) future.get();
                recommendations.addAll(list);
            } catch (ExecutionException e) {
                log.info("Error fetching recommendations: {}", e.getCause().getMessage());
            }
        }
        return recommendations.stream().distinct().map(Object::toString).collect(Collectors.joining(" \n"));
    }

    public List<Book> getRecommendations(Book orderedBook) {
        log.info("Thread {} started fetching recommendations for {} ", Thread.currentThread().getName(), orderedBook.getTitle());
        try {
            // Simulate database query delay
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        List<Book> recommendations = bookRepository.findTop3ByGenreOrderByTitleAsc(orderedBook.getGenre());
        recommendations.forEach(book -> log.info("recommended {}", book));
        log.info("Thread {} completed fetching recommendations for the book {} ", Thread.currentThread().getName(), orderedBook.getTitle());
        return recommendations;
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
