package com.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bookshop.model.Item;
@Repository
public interface OrderItemRepository extends JpaRepository<Item, Integer> {
}
