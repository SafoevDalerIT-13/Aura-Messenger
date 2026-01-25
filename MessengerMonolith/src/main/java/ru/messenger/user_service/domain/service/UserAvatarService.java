package ru.messenger.user_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserAvatarService {

    @Value("${file.upload-dir}")
    private String uploadDir;

    private final UserRepository userRepository;

    private Path getUploadsPath() {
        Path path = Paths.get(uploadDir);
        if (!path.isAbsolute()) {
            path = Paths.get("").toAbsolutePath().resolve(uploadDir);
        }
        return path;
    }

    public String uploadAvatar(String login, MultipartFile file) throws IOException {
        validateFile(file);

        Path uploadPath = getUploadsPath();
        log.info("Путь для загрузки файлов: {}", uploadPath.toAbsolutePath());

        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Создана директория: {}", uploadPath.toAbsolutePath());
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String filename = UUID.randomUUID() + extension;

        Path filePath = uploadPath.resolve(filename);
        file.transferTo(filePath.toFile());

        log.info("Файл сохранен: {}", filePath.toAbsolutePath());

        String avatarUrl = "/uploads/" + filename;

        updateUserAvatar(login, avatarUrl);

        log.info("Аватар загружен для {}: {}", login, avatarUrl);
        return avatarUrl;
    }

    public void deleteAvatar(String login) throws IOException {
        UserEntity user = userRepository.findByLogin(login) // ← исправлено на findByLogin
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getAvatarUrl() != null) {
            deleteOldFile(user.getAvatarUrl());
            user.setAvatarUrl(null);
            userRepository.save(user);
            log.info("Аватар удален для пользователя: {}", login); // ← логируем login
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Файл пустой");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Только изображения разрешены");
        }

        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Файл слишком большой (макс. 5MB)");
        }
    }

    private void updateUserAvatar(String login, String avatarUrl) {
        UserEntity user = userRepository.findByLogin(login) // ← исправлено на findByLogin
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (user.getAvatarUrl() != null) {
            try {
                deleteOldFile(user.getAvatarUrl());
            } catch (IOException e) {
                log.warn("Не удалось удалить старую аватарку: {}", e.getMessage());
            }
        }

        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
    }

    private void deleteOldFile(String avatarUrl) throws IOException {
        if (avatarUrl != null && avatarUrl.startsWith("/uploads/")) {
            String filename = avatarUrl.substring("/uploads/".length());
            Path filePath = getUploadsPath().resolve(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
        }
    }
}