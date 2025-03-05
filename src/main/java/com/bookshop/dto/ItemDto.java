package com.bookshop.dto;

import lombok.Builder;
import lombok.Data;
@Data
@Builder
public class ItemDto {
    private int id;

    private int bookId;
    private int quantity;
}
