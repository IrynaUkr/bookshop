package com.bookshop.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bookshop.bookTransferService.BookTransferService;
import com.bookshop.dto.BookOrderRequest;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@RestController
@RequestMapping("api/order")
public class BookOrderController {
    private final BookTransferService bookTransferService;

    @PostMapping("/books")
    public ResponseEntity<String> transferBooksBatch(@RequestBody BookOrderRequest request) {
        try {
            bookTransferService.transferBooks(request);
            return ResponseEntity.ok("order placed successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
