package com.bookshop.controller;

import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bookshop.dto.AuthResponseDto;
import com.bookshop.dto.UserDto;
import com.bookshop.model.Role;
import com.bookshop.model.UserEntity;
import com.bookshop.repository.RoleRepository;
import com.bookshop.repository.UserRepository;
import com.bookshop.security.JwtGenerator;
import lombok.AllArgsConstructor;


@RestController
@RequestMapping("/auth")
@AllArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final JwtGenerator generator;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody UserDto userDto) {
        if (userRepository.existsByName(userDto.getUsername())) {
            return new ResponseEntity<>("username is taken", HttpStatus.BAD_REQUEST);
        }

        UserEntity userEntity = new UserEntity();
        userEntity.setName(userDto.getUsername());
        userEntity.setPassword(passwordEncoder.encode(userDto.getPassword()));
        Role role = roleRepository.getRoleByName("USER").orElseThrow(() -> new RuntimeException("Role not found"));
        userEntity.setRoles(Collections.singletonList(role));
        userRepository.save(userEntity);

        return new ResponseEntity<>("user added successfully", HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@RequestBody UserDto userDto) {
        Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(userDto.getUsername(), userDto.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String generatedToken = generator.generateToken(authentication);

        return new ResponseEntity<>(new AuthResponseDto(generatedToken), HttpStatus.OK);
    }
}
