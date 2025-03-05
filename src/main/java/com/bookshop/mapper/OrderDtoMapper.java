package com.bookshop.mapper;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.bookshop.dto.ItemDto;
import com.bookshop.dto.OrderDto;
import com.bookshop.model.Item;
import com.bookshop.model.Order;
@Service
public class OrderDtoMapper {

    public OrderDto mapOrderDto(Order order) {
        return OrderDto.builder()
                .id(order.getId())
                .userName(order.getUser().getName())
                .orderDate(order.getOrderDate())
                .items(order.getItems() != null
                        ? order.getItems().stream().map(this::mapItemDto).collect(Collectors.toList())
                        : Collections.emptyList())
                .build();
    }

    public ItemDto mapItemDto(Item item) {
        return ItemDto.builder()
                .id(item.getId())
                .bookId(item.getBookId())
                .quantity(item.getQuantity())
                .build();
    }
}

