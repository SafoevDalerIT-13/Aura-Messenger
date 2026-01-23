package ru.messenger.config_service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth
                        // Публичные страницы
                        .requestMatchers(
                                "/auth.html",
                                "/login.html",
                                "/api/v1/users/auth",
                                "/error"
                        ).permitAll()

                        // ВСЁ остальное требует аутентификации
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginPage("/login.html")                 // показываем эту страницу
                        .loginProcessingUrl("/api/v1/auth/login") // POST сюда
                        .defaultSuccessUrl("/home.html", true)    // после успешного входа
                        .failureUrl("/login.html?error=true")     // при ошибке
                        .usernameParameter("username")            // имя поля в форме
                        .passwordParameter("password")            // имя поля в форме
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessUrl("/login.html?logout")
                        .permitAll()
                )

                .httpBasic(httpBasic -> httpBasic.disable())

                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}