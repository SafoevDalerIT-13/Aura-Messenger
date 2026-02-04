package ru.messenger.group_service.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import ru.messenger.group_service.api.dto.request.GroupPostCreateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupPostResponseDto;
import ru.messenger.group_service.domain.service.GroupPostService;
import ru.messenger.user_service.domain.service.UserService;

@RestController
@RequestMapping("/api/v1/groups/posts")
@RequiredArgsConstructor
public class GroupPostController {

    private final GroupPostService groupPostService;
    private final UserService userService;

    /**
     * Создать пост в группе
     *
     * @param requestDto данные для создания поста
     * @param userDetails информация о текущем пользователе
     * @return созданный пост
     */
    @PostMapping
    public ResponseEntity<GroupPostResponseDto> createPost(
            @Valid @RequestBody GroupPostCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupPostResponseDto response = groupPostService.createPost(requestDto, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить информацию о посте
     *
     * @param postId ID поста
     * @param userDetails информация о текущем пользователе
     * @return информация о посте
     */
    @GetMapping("/{postId}")
    public ResponseEntity<GroupPostResponseDto> getPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupPostResponseDto response = groupPostService.getPost(postId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить посты группы
     *
     * @param groupId ID группы
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с постами группы
     */
    @GetMapping("/group/{groupId}")
    public ResponseEntity<Page<GroupPostResponseDto>> getGroupPosts(
            @PathVariable Long groupId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupPostResponseDto> response = groupPostService.getGroupPosts(groupId, userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить ленту постов из всех групп пользователя
     *
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с постами из ленты
     */
    @GetMapping("/feed")
    public ResponseEntity<Page<GroupPostResponseDto>> getFeed(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupPostResponseDto> response = groupPostService.getUserFeed(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить пост
     *
     * @param postId ID поста
     * @param content новый текст поста
     * @param userDetails информация о текущем пользователе
     * @return обновленный пост
     */
    @PutMapping("/{postId}")
    public ResponseEntity<GroupPostResponseDto> updatePost(
            @PathVariable Long postId,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupPostResponseDto response = groupPostService.updatePost(postId, content, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Удалить пост
     *
     * @param postId ID поста
     * @param userDetails информация о текущем пользователе
     * @return пустой ответ
     */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        groupPostService.deletePost(postId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Поставить или убрать лайк с поста
     *
     * @param postId ID поста
     * @param userDetails информация о текущем пользователе
     * @return информация о посте с обновленным статусом лайка
     */
    @PostMapping("/{postId}/like")
    public ResponseEntity<GroupPostResponseDto> toggleLike(
            @PathVariable Long postId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupPostResponseDto response = groupPostService.toggleLike(postId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить посты определенного типа в группе
     *
     * @param groupId ID группы
     * @param type тип поста (POST, ANNOUNCEMENT, POLL, EVENT, MEDIA)
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с постами указанного типа
     */
    @GetMapping("/group/{groupId}/type/{type}")
    public ResponseEntity<Page<GroupPostResponseDto>> getPostsByType(
            @PathVariable Long groupId,
            @PathVariable ru.messenger.group_service.domain.entity.enums.GroupPostType type,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupPostResponseDto> response = groupPostService.getPostsByType(groupId, type, userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить посты пользователя во всех группах
     *
     * @param authorId ID автора (опционально, если не указан - текущий пользователь)
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с постами пользователя
     */
    @GetMapping("/author/{authorId}")
    public ResponseEntity<Page<GroupPostResponseDto>> getPostsByAuthor(
            @PathVariable(required = false) Long authorId,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentUserId = userService.getCurrentUserId(userDetails);
        Long targetAuthorId = authorId != null ? authorId : currentUserId;

        Page<GroupPostResponseDto> response = groupPostService.getPostsByAuthor(targetAuthorId, currentUserId, pageable);
        return ResponseEntity.ok(response);
    }
}