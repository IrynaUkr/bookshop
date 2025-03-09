package com.bookshop.dto;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ItemDto {
    private Long id;

    private Long bookId;
    private int quantity;
}
