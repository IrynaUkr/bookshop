package com.bookshop.dto;

import java.util.List;
import com.bookshop.model.Item;
import lombok.Data;

@Data
public class BookOrderRequest {

    private int userId;
    private List<Item> orderItems;
}
