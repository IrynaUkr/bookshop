INSERT INTO roles (id, name)
VALUES (1, 'admin'),
       (2, 'customer'),
       (3, 'manager');

INSERT INTO users (name, password)
VALUES ('user1', 'password1'),
       ('user2', 'password2');

INSERT INTO book (author, stock, title, genre)
VALUES
-- Nonfiction (Popular Java & Spring Boot Books)
('Joshua Bloch', 100, 'Effective Java', 'Nonfiction'),
('Herbert Schildt', 100, 'Java: The Complete Reference', 'Nonfiction'),
('Kathy Sierra & Bert Bates', 100, 'Head First Java', 'Nonfiction'),
('Robert C. Martin', 100, 'Clean Code', 'Nonfiction'),
('Craig Walls', 100, 'Spring in Action', 'Nonfiction'),
('Ranga Rao Karanam', 100, 'Mastering Spring Boot 3.0', 'Nonfiction'),
('Mark Heckler', 100, 'Spring Boot: Up and Running', 'Nonfiction'),
('Juergen Hoeller', 100, 'Spring Framework in Action', 'Nonfiction'),
('Venkat Subramaniam', 100, 'Functional Programming in Java', 'Nonfiction'),
('Brian Goetz', 100, 'Java Concurrency in Practice', 'Nonfiction'),

-- Fiction (Top 10 Popular Fiction Books)
('J.K. Rowling', 100, 'Harry Potter ', 'Fiction'),
('George Orwell', 100, '1984', 'Fiction'),
('J.R.R. Tolkien', 100, 'The Lord of the Rings', 'Fiction'),
('Harper Lee', 100, 'To Kill a Mockingbird', 'Fiction'),
('F. Scott Fitzgerald', 100, 'The Great Gatsby', 'Fiction'),
('Jane Austen', 100, 'Pride and Prejudice', 'Fiction'),
('Leo Tolstoy', 100, 'War and Peace', 'Fiction'),
('Markus Zusak', 100, 'The Book Thief', 'Fiction'),
('Dan Brown', 100, 'The Da Vinci Code', 'Fiction'),
('Gabriel García Márquez', 100, 'One Hundred Years of Solitude', 'Fiction'),

-- Fairy Tale (Top 10 Classic Fairy Tales)
('Brothers Grimm', 100, 'Cinderella', 'Fairy Tale'),
('Brothers Grimm', 100, 'Snow White and the Seven Dwarfs', 'Fairy Tale'),
('Hans Christian Andersen', 100, 'The Little Mermaid', 'Fairy Tale'),
('Hans Christian Andersen', 100, 'The Ugly Duckling', 'Fairy Tale'),
('Charles Perrault', 100, 'Sleeping Beauty', 'Fairy Tale'),
('Hans Christian Andersen', 100, 'The Snow Queen', 'Fairy Tale'),
('Aesop', 100, 'The Tortoise and the Hare', 'Fairy Tale'),
('Hans Christian Andersen', 100, 'Thumbelina', 'Fairy Tale'),
('Brothers Grimm', 100, 'Hansel and Gretel', 'Fairy Tale'),
('Brothers Grimm', 100, 'Rumpelstiltskin', 'Fairy Tale');

-----------------------------
