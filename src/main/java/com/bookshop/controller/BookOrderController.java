package com.bookshop.controller;

import com.bookshop.dto.BookOrderRequest;
import com.bookshop.service.BookTransferService;
import com.bookshop.service.BookTransferServiceFuture;
import com.bookshop.service.RecommendationService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@AllArgsConstructor
@RestController
@RequestMapping("api/order")
public class BookOrderController {
    private final BookTransferService bookTransferService;
    private final BookTransferServiceFuture bookTransferServiceFuture;
    private final RecommendationService recommendationService;


    @PostMapping("/create")
    public ResponseEntity<String> transferBooks(@RequestBody BookOrderRequest request) {
        log.info("BookOrderRequest received");
        try {
            bookTransferService.transferBooks(request);
            String recommendations = recommendationService.getRecommendations(request);

            return ResponseEntity.ok("order placed successfully.Consider recommendations:" + recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping("/create/future")
    public ResponseEntity<String> transferBooksFuture(@RequestBody BookOrderRequest request) {
        try {
            log.info("BookOrderRequest future received");
            bookTransferServiceFuture.transferBooks(request);
            return ResponseEntity.ok("order placed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

}
