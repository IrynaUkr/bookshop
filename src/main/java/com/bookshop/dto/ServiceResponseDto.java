package com.bookshop.dto;

import lombok.Data;
@Data
public class ServiceResponseDto {
    OrderDto order;
    ErrorDto errorDto;
}
