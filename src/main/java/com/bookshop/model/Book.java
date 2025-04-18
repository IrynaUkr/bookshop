package com.bookshop.model;

import java.io.Serializable;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@EqualsAndHashCode
public class Book implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String author;
    private int stock; // Current stock quantity
    private String genre;

    @Override
    public String toString() {
        return "title: " + getTitle() + " author: " + getAuthor() + " genre: " + getGenre() + "stock: " + getStock();
    }

}
