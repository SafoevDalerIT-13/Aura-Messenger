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
import ru.messenger.group_service.api.dto.request.GroupCommentCreateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupCommentResponseDto;
import ru.messenger.group_service.domain.service.GroupCommentService;
import ru.messenger.user_service.domain.service.UserService;

@RestController
@RequestMapping("/api/v1/groups/posts/{postId}/comments")
@RequiredArgsConstructor
public class GroupCommentController {

    private final GroupCommentService groupCommentService;
    private final UserService userService;

    /**
     * Создать комментарий к посту
     *
     * @param postId ID поста
     * @param requestDto данные комментария
     * @param userDetails информация о текущем пользователе
     * @return созданный комментарий
     */
    @PostMapping
    public ResponseEntity<GroupCommentResponseDto> createComment(
            @PathVariable Long postId,
            @Valid @RequestBody GroupCommentCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupCommentResponseDto response = groupCommentService.createComment(postId, requestDto, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить комментарии к посту
     *
     * @param postId ID поста
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с комментариями
     */
    @GetMapping
    public ResponseEntity<Page<GroupCommentResponseDto>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupCommentResponseDto> response = groupCommentService.getComments(postId, userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить комментарий
     *
     * @param postId ID поста
     * @param commentId ID комментария
     * @param content новый текст комментария
     * @param userDetails информация о текущем пользователе
     * @return обновленный комментарий
     */
    @PutMapping("/{commentId}")
    public ResponseEntity<GroupCommentResponseDto> updateComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @RequestParam String content,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupCommentResponseDto response = groupCommentService.updateComment(commentId, content, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Удалить комментарий
     *
     * @param postId ID поста
     * @param commentId ID комментария
     * @param userDetails информация о текущем пользователе
     * @return пустой ответ
     */
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        groupCommentService.deleteComment(commentId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Поставить или убрать лайк с комментария
     *
     * @param postId ID поста
     * @param commentId ID комментария
     * @param userDetails информация о текущем пользователе
     * @return информация о комментарии с обновленным статусом лайка
     */
    @PostMapping("/{commentId}/like")
    public ResponseEntity<GroupCommentResponseDto> toggleCommentLike(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupCommentResponseDto response = groupCommentService.toggleLike(commentId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить ответы на комментарий
     *
     * @param postId ID поста
     * @param commentId ID родительского комментария
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с ответами на комментарий
     */
    @GetMapping("/{commentId}/replies")
    public ResponseEntity<Page<GroupCommentResponseDto>> getCommentReplies(
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupCommentResponseDto> response = groupCommentService.getReplies(commentId, userId, pageable);
        return ResponseEntity.ok(response);
    }
}