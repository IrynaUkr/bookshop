package com.bookshop.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity

public class SecurityConfig {
//    private final UserDetailsService userDetailsService;
//
//    @Autowired
//    public SecurityConfig(UserDetailsService personDetailsService) {
//        this.userDetailsService = personDetailsService;
//    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity.csrf(Customizer.withDefaults())
                .authorizeHttpRequests(auth ->
                        auth.requestMatchers("/api").permitAll()
                                .requestMatchers("/api/**")
                                .authenticated()
                )
                .httpBasic(Customizer.withDefaults()).build();
    }

//    @Bean
//    public UserDetailsService userDetailsService() {
//        UserDetails admin = User.builder()
//                .password("123")
//                .username("admin")
//                .roles("ADMIN")
//                .build();
//        UserDetails user = User.builder()
//                .password("12345")
//                .username("user")
//                .roles("USER")
//                .build();
//        return new InMemoryUserDetailsManager(admin, user);
//    }

}
