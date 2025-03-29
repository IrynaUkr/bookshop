package com.bookshop.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.dto.OrderDto;
import com.bookshop.exception.InsufficientStockException;
import com.bookshop.exception.OrderNotFoundException;
import com.bookshop.exception.ProductNotFoundException;
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

    private static ResponseEntity<OrderDto> getOrderDtoWithErrorResponseEntity(Throwable ex) {
        HttpStatus status;
        String errorMessage;
        if (ex.getCause() instanceof InsufficientStockException) {
            status = HttpStatus.BAD_REQUEST;
            errorMessage = ex.getCause().getMessage();
        } else if (ex.getCause() instanceof OrderNotFoundException || ex.getCause() instanceof ProductNotFoundException) {
            status = HttpStatus.NOT_FOUND;
            errorMessage = ex.getCause().getMessage();
        } else {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
            errorMessage = " An unexpected error occurred.";
        }
        OrderDto errorOrder = new OrderDto();
        errorOrder.setErrorDetails(status.getReasonPhrase() + errorMessage);

        return ResponseEntity.status(status).body(errorOrder);
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody BookOrderRequest orderRequest) {
        log.info("new book order request received : [ {}]", orderRequest);
        try {
            return new ResponseEntity<>(orderService.createOrder(orderRequest), HttpStatus.CREATED);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Order was not created: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrder(@PathVariable Long id) {
        log.info("get order by id {} received", id);
        try {
            return new ResponseEntity<>(orderService.getOrder(id), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Order was not found: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOrder(@PathVariable Long id) {
        log.info("Delete order by id {} received", id);
        try {
            orderService.deleteOrder(id);
            log.info("Order with id {}  was deleted  ", id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Order was not deleted: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    @PutMapping("/{id}")
    public CompletableFuture<ResponseEntity<OrderDto>> updateOrder(@RequestBody BookOrderRequest orderRequest,
                                                                   @PathVariable Long id) {
        log.info("Update bookOrderRequest received");
        return orderService.updateOrder(id, orderRequest)
                .thenApply(ResponseEntity::ok)
                .exceptionally(ex -> {
                    log.error("Error updating order: {}", ex.getMessage());
                    return getOrderDtoWithErrorResponseEntity(ex);
                });
    }

}
