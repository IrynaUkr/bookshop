package com.bookshop.dto;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@AllArgsConstructor
@NoArgsConstructor
public class ErrorDto extends ServiceResponseDto{
    private String error;
    private String message;
    private Instant timestamp;
}
