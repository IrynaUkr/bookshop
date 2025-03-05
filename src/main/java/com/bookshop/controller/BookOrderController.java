package com.bookshop.controller;

import java.util.concurrent.CompletableFuture;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.model.Order;
import com.bookshop.service.OrderService;
import com.bookshop.service.RecommendationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("api/order")
public class BookOrderController {
    private final OrderService orderService;
    private final RecommendationService recommendationService;

    @PostMapping
    public CompletableFuture<ResponseEntity<Order>> createOrder(@RequestBody BookOrderRequest orderRequest) {
        log.info("BookOrderRequest received " + orderRequest);
        return orderService.createOrder(orderRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error processing order: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null);
                });
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable Integer id) {
        log.info("get order by id {} received", id);
        try {
            return new ResponseEntity<>(orderService.getOrder(id), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Order was not found: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Integer id) {
        log.info("delete order by id {} received", id);
        try {
            orderService.deleteOrder(id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Order was not deleted: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping
    public CompletableFuture<ResponseEntity<Order>> updateOrder(@RequestBody BookOrderRequest orderRequest,
                                                                @PathVariable Integer orderId) {
        log.info("Update bookOrderRequest received");
        return orderService.updateOrder(orderId, orderRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error updating order: {}", ex.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(null);
                });
    }

}
