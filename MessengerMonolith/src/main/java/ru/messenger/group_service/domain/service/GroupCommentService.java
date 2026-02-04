package ru.messenger.group_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.group_service.api.dto.request.GroupCommentCreateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupCommentResponseDto;
import ru.messenger.group_service.api.mapper.GroupCommentMapper;
import ru.messenger.group_service.domain.entity.GroupPostCommentEntity;
import ru.messenger.group_service.domain.entity.GroupPostEntity;
import ru.messenger.group_service.domain.entity.enums.GroupMemberStatus;
import ru.messenger.group_service.domain.entity.enums.GroupPostStatus;
import ru.messenger.group_service.domain.repository.GroupCommentRepository;
import ru.messenger.group_service.domain.repository.GroupMemberRepository;
import ru.messenger.group_service.domain.repository.GroupPostRepository;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupCommentService {

    private final GroupCommentRepository groupCommentRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupCommentMapper groupCommentMapper;

    /**
     * Создать комментарий к посту
     */
    @Transactional
    public GroupCommentResponseDto createComment(Long postId, GroupCommentCreateRequestDto requestDto, Long userId) {
        log.info("Создание комментария к посту {} пользователем {}", postId, userId);

        // 1. Находим пост
        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // 2. Проверяем, что пост опубликован
        if (!post.isPublished()) {
            throw new RuntimeException("Нельзя комментировать неопубликованный пост");
        }

        // 3. Находим автора
        UserEntity author = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 4. Проверяем доступ к группе
        checkGroupAccess(post.getGroup(), userId);

        // 5. Проверяем, является ли пользователь участником группы
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserIdAndStatus(
                post.getGroup().getId(), userId, GroupMemberStatus.ACTIVE);

        if (!isMember) {
            throw new RuntimeException("Только участники группы могут оставлять комментарии");
        }

        // 6. Создаем комментарий
        GroupPostCommentEntity comment = GroupPostCommentEntity.builder()
                .post(post)
                .author(author)
                .content(requestDto.getContent())
                .build();

        // 7. Если есть родительский комментарий
        if (requestDto.getParentCommentId() != null) {
            GroupPostCommentEntity parentComment = groupCommentRepository.findById(requestDto.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Родительский комментарий не найден"));

            // Проверяем, что родительский комментарий относится к тому же посту
            if (!parentComment.getPost().getId().equals(postId)) {
                throw new RuntimeException("Родительский комментарий относится к другому посту");
            }

            comment.setParentComment(parentComment);
        }

        // 8. Сохраняем комментарий
        GroupPostCommentEntity savedComment = groupCommentRepository.save(comment);

        // 9. Обновляем счетчик комментариев в посте
        updateCommentsCount(postId);

        log.info("Создан комментарий ID: {}", savedComment.getId());

        return enrichWithLikedByMe(groupCommentMapper.toResponseDto(savedComment), userId);
    }

    /**
     * Получить комментарии к посту
     */
    @Transactional(readOnly = true)
    public Page<GroupCommentResponseDto> getComments(Long postId, Long userId, Pageable pageable) {
        log.info("Получение комментариев к посту {} для пользователя {}", postId, userId);

        // 1. Находим пост
        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // 2. Проверяем доступ к группе
        checkGroupAccess(post.getGroup(), userId);

        // 3. Получаем комментарии (только корневые, без ответов)
        Page<GroupPostCommentEntity> comments = groupCommentRepository.findByPostIdAndParentCommentIsNull(
                postId, pageable);

        // 4. Обогащаем информацией о лайках
        return comments.map(comment -> enrichWithLikedByMe(
                groupCommentMapper.toResponseDto(comment), userId));
    }

    /**
     * Обновить комментарий
     */
    @Transactional
    public GroupCommentResponseDto updateComment(Long commentId, String content, Long userId) {
        log.info("Обновление комментария {} пользователем {}", commentId, userId);

        // 1. Находим комментарий
        GroupPostCommentEntity comment = groupCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // 2. Проверяем, что пользователь является автором
        if (!comment.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Только автор может редактировать комментарий");
        }

        // 3. Проверяем, что не прошло слишком много времени (например, 15 минут)
        if (comment.getCreatedAt().plusSeconds(900).isBefore(Instant.now())) {
            throw new RuntimeException("Редактирование комментария доступно только в течение 15 минут после создания");
        }

        // 4. Обновляем содержание
        comment.setContent(content);
        comment.setUpdatedAt(Instant.now());

        GroupPostCommentEntity updatedComment = groupCommentRepository.save(comment);

        return enrichWithLikedByMe(groupCommentMapper.toResponseDto(updatedComment), userId);
    }

    /**
     * Удалить комментарий
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        log.info("Удаление комментария {} пользователем {}", commentId, userId);

        // 1. Находим комментарий
        GroupPostCommentEntity comment = groupCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // 2. Находим пост
        GroupPostEntity post = comment.getPost();

        // 3. Проверяем права
        boolean canDelete = false;

        // Автор может удалить свой комментарий
        if (comment.getAuthor().getId().equals(userId)) {
            canDelete = true;
        }
        // Администраторы/модераторы группы могут удалять любые комментарии
        else {
            var member = groupMemberRepository.findByGroupIdAndUserId(post.getGroup().getId(), userId);
            if (member.isPresent() && (member.get().getCanManagePosts() || member.get().getIsAdmin())) {
                canDelete = true;
            }
        }

        if (!canDelete) {
            throw new RuntimeException("Нет прав для удаления комментария");
        }

        // 4. Если у комментария есть ответы, помечаем его как удаленный
        if (!comment.getReplies().isEmpty()) {
            comment.setContent("[Комментарий удален]");
            comment.setUpdatedAt(Instant.now());
            groupCommentRepository.save(comment);
        }
        // Если ответов нет, удаляем полностью
        else {
            groupCommentRepository.delete(comment);
        }

        // 5. Обновляем счетчик комментариев в посте
        updateCommentsCount(post.getId());
    }

    /**
     * Поставить/убрать лайк на комментарий
     */
    @Transactional
    public GroupCommentResponseDto toggleLike(Long commentId, Long userId) {
        log.info("Переключение лайка на комментарий {} пользователем {}", commentId, userId);

        // 1. Находим комментарий
        GroupPostCommentEntity comment = groupCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // 2. Находим пользователя
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 3. Проверяем доступ к группе
        checkGroupAccess(comment.getPost().getGroup(), userId);

        // 4. Проверяем, является ли пользователь участником группы
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserIdAndStatus(
                comment.getPost().getGroup().getId(), userId, GroupMemberStatus.ACTIVE);

        if (!isMember) {
            throw new RuntimeException("Только участники группы могут ставить лайки");
        }

        // 5. Ищем существующий лайк
        Optional<ru.messenger.group_service.domain.entity.GroupCommentLikeEntity> existingLike =
                comment.getLikes().stream()
                        .filter(like -> like.getUser().getId().equals(userId))
                        .findFirst();

        if (existingLike.isPresent()) {
            // Убираем лайк
            comment.getLikes().remove(existingLike.get());
            comment.setLikesCount(Math.max(0, comment.getLikesCount() - 1));
        } else {
            // Ставим лайк
            ru.messenger.group_service.domain.entity.GroupCommentLikeEntity like =
                    ru.messenger.group_service.domain.entity.GroupCommentLikeEntity.builder()
                            .comment(comment)
                            .user(user)
                            .build();
            comment.getLikes().add(like);
            comment.setLikesCount(comment.getLikesCount() + 1);
        }

        GroupPostCommentEntity updatedComment = groupCommentRepository.save(comment);

        return enrichWithLikedByMe(groupCommentMapper.toResponseDto(updatedComment), userId);
    }

    /**
     * Получить ответы на комментарий
     */
    @Transactional(readOnly = true)
    public Page<GroupCommentResponseDto> getReplies(Long commentId, Long userId, Pageable pageable) {
        log.info("Получение ответов на комментарий {} для пользователя {}", commentId, userId);

        // 1. Находим родительский комментарий
        GroupPostCommentEntity parentComment = groupCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Комментарий не найден"));

        // 2. Проверяем доступ к группе
        checkGroupAccess(parentComment.getPost().getGroup(), userId);

        // 3. Получаем ответы
        Page<GroupPostCommentEntity> replies = groupCommentRepository.findByParentCommentId(
                commentId, pageable);

        // 4. Обогащаем информацией о лайках
        return replies.map(reply -> enrichWithLikedByMe(
                groupCommentMapper.toResponseDto(reply), userId));
    }

    /**
     * Получить количество комментариев у поста
     */
    @Transactional(readOnly = true)
    public long getCommentsCount(Long postId, Long userId) {
        log.info("Получение количества комментариев у поста {} для пользователя {}", postId, userId);

        // 1. Находим пост
        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // 2. Проверяем доступ
        checkGroupAccess(post.getGroup(), userId);

        // 3. Возвращаем количество
        return groupCommentRepository.countByPostId(postId);
    }

    /**
     * Получить комментарии пользователя в группе
     */
    @Transactional(readOnly = true)
    public Page<GroupCommentResponseDto> getUserCommentsInGroup(Long groupId, Long targetUserId,
                                                                Long currentUserId, Pageable pageable) {
        log.info("Получение комментариев пользователя {} в группе {} для пользователя {}",
                targetUserId, groupId, currentUserId);

        // 1. Проверяем доступ к группе
        checkGroupAccess(groupId, currentUserId);

        // 2. Получаем комментарии
        Page<GroupPostCommentEntity> comments = groupCommentRepository.findByGroupIdAndAuthorId(
                groupId, targetUserId, pageable);

        // 3. Обогащаем информацией о лайках
        return comments.map(comment -> enrichWithLikedByMe(
                groupCommentMapper.toResponseDto(comment), currentUserId));
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Обновить счетчик комментариев в посте
     */
    private void updateCommentsCount(Long postId) {
        long count = groupCommentRepository.countByPostId(postId);
        groupPostRepository.findById(postId).ifPresent(post -> {
            post.setCommentsCount((int) count);
            groupPostRepository.save(post);
        });
    }

    /**
     * Обогатить DTO информацией о лайке пользователя
     */
    private GroupCommentResponseDto enrichWithLikedByMe(GroupCommentResponseDto dto, Long userId) {
        if (userId != null) {
            groupCommentRepository.findById(dto.getId()).ifPresent(comment -> {
                boolean likedByMe = comment.getLikes().stream()
                        .anyMatch(like -> like.getUser().getId().equals(userId));
                dto.setLikedByMe(likedByMe);
            });
        }
        return dto;
    }

    /**
     * Проверить доступ к группе
     */
    private void checkGroupAccess(Long groupId, Long userId) {
        // Этот метод нужно реализовать в GroupService или использовать существующий
        // Пока упрощенная версия
        boolean hasAccess = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!hasAccess) {
            throw new RuntimeException("Доступ к группе запрещен");
        }
    }

    private void checkGroupAccess(ru.messenger.group_service.domain.entity.GroupEntity group, Long userId) {
        checkGroupAccess(group.getId(), userId);
    }
}