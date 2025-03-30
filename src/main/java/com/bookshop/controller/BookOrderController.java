package com.bookshop.controller;

import java.util.concurrent.ExecutionException;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.dto.OrderDto;
import com.bookshop.exception.InsufficientStockException;
import com.bookshop.exception.OrderNotFoundException;
import com.bookshop.exception.ProductNotFoundException;
import com.bookshop.service.OrderService;
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

    public static void handleError(Throwable e) {
        Throwable cause = e.getCause();
        if (cause instanceof ProductNotFoundException) {
            log.error("Order processing ProductNotFoundException: {}", cause.getMessage());
            throw (ProductNotFoundException) cause;
        }
        if (cause instanceof InsufficientStockException) {
            log.error("Order processing InsufficientStockException: {}", cause.getMessage());
            throw (InsufficientStockException) cause;
        }
        if (cause instanceof OrderNotFoundException) {
            log.error("Order processing OrderNotFoundException: {}", cause.getMessage());
            throw (OrderNotFoundException) cause;
        }
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody BookOrderRequest orderRequest)
            throws ExecutionException, InterruptedException {
        try {
            return new ResponseEntity<>(orderService.createOrder(orderRequest), HttpStatus.CREATED);
        } catch (ExecutionException e) {
            handleError(e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
            return new ResponseEntity<>(orderService.getOrder(id), HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        orderService.deleteOrder(id);
        log.info("Order with id {}  was deleted  ", id);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PutMapping("/{id}")
    public ResponseEntity<OrderDto> updateOrder(@RequestBody BookOrderRequest orderRequest,
                                                @PathVariable Long id) throws ExecutionException, InterruptedException {
        log.info("Update bookOrderRequest received");
        try {
            return new ResponseEntity<>(orderService.updateOrder(id, orderRequest), HttpStatus.OK);
        } catch (ExecutionException e) {
            handleError(e);
            throw e;
        }
    }
}
