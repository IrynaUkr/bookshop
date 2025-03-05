package com.bookshop.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto{
    private Long id;

    private String userName;

    private LocalDateTime orderDate;

    private List<ItemDto> items;

    private String errorDetails;

}
