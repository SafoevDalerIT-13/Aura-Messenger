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
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Ищем пользователя по username
        UserEntity user = userRepository.findByIdentifier(identifier)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Неверный логин/телефон/email или пароль"
                ));

        // Создаем UserDetails из найденного пользователя
        return User.builder()
                .username(user.getLogin()) // username для Spring Security
                .password(user.getPassword()) // уже закодированный пароль
                .roles("USER") // роль пользователя
                .build();
    }
}