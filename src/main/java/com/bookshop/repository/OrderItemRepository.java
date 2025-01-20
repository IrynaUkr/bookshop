package com.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bookshop.model.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
}
