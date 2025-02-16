package com.bookshop.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class Book {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String title;
    private String author;
    private int stock; // Current stock quantity
    private String genre;

    @Override
    public String toString() {
        return "title: "+getTitle() + " author: " + getAuthor() + " genre: " + getGenre();
    }

}
