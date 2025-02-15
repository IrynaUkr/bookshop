package com.bookshop.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bookshop.model.Item;
@Repository
public interface ItemRepository extends JpaRepository<Item, Integer> {
}
