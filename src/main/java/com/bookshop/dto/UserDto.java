package com.bookshop.dto;

import lombok.Data;

import java.io.Serializable;
import com.bookshop.model.Role;

@Data
public class UserDto implements Serializable {
    private String username;
    private String password;
    private Role role;
}
