package com.bookshop.controller;

import java.util.concurrent.ExecutionException;
import com.bookshop.dto.BookOrderRequest;
import com.bookshop.dto.OrderDto;
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

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@RequestBody BookOrderRequest orderRequest) throws ExecutionException, InterruptedException {
        return new ResponseEntity<>(orderService.createOrder(orderRequest), HttpStatus.CREATED);
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
    public ResponseEntity<OrderDto> updateOrder(@RequestBody BookOrderRequest orderRequest,
                                                                   @PathVariable Long id) {
        log.info("Update bookOrderRequest received");
        try {
            return new ResponseEntity<>(orderService.updateOrder(id, orderRequest), HttpStatus.OK);
        } catch (ExecutionException | InterruptedException e) {
            log.error("Order was not modified: {}", e.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);
    }

}
