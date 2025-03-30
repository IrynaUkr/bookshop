package com.bookshop.exception.handler;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.bookshop.exception.InsufficientStockException;
import com.bookshop.exception.OrderNotFoundException;
import com.bookshop.exception.ProductNotFoundException;

@RestControllerAdvice
public class ExceptionHandlerGlobal {

    @ExceptionHandler({InsufficientStockException.class, OrderNotFoundException.class, ProductNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<String> handleNotFoundException(RuntimeException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<String> handleGenericException(Exception ex) {
        return new ResponseEntity<>("An unexpected error occurred: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
