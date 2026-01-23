package ru.messenger.config_service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Ищем пользователя по username
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Пользователь '" + username + "' не найден"
                ));

        // Создаем UserDetails из найденного пользователя
        return User.builder()
                .username(user.getUsername()) // username для Spring Security
                .password(user.getPassword()) // уже закодированный пароль
                .roles("USER") // роль пользователя
                .build();
    }
}