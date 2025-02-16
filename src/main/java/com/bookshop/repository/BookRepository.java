package com.bookshop.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.bookshop.model.Book;
@Repository
public interface BookRepository extends JpaRepository<Book, Integer> {
    Optional<Book> findBookById(int id);
    List<Book> findTop3ByGenreOrderByTitleAsc(String genre);
}
