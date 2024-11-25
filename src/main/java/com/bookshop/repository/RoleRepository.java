package com.bookshop.repository;

import com.bookshop.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> getRoleByName(String name);
}
