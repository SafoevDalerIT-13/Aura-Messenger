package ru.messenger.group_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.group_service.api.dto.request.GroupPostCreateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupPostResponseDto;
import ru.messenger.group_service.api.mapper.GroupMapper;
import ru.messenger.group_service.domain.entity.GroupEntity;
import ru.messenger.group_service.domain.entity.GroupMemberEntity;
import ru.messenger.group_service.domain.entity.GroupPostEntity;
import ru.messenger.group_service.domain.entity.GroupPostLikeEntity;
import ru.messenger.group_service.domain.entity.enums.GroupPostStatus;
import ru.messenger.group_service.domain.entity.enums.GroupPostType;
import ru.messenger.group_service.domain.repository.GroupMemberRepository;
import ru.messenger.group_service.domain.repository.GroupPostRepository;
import ru.messenger.group_service.domain.repository.GroupRepository;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupPostService {

    private final GroupPostRepository groupPostRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    /**
     * Создать пост в группе
     */
    @Transactional
    public GroupPostResponseDto createPost(GroupPostCreateRequestDto requestDto, Long authorId) {
        log.info("Создание поста в группе ID: {} пользователем {}", requestDto.getGroupId(), authorId);

        GroupEntity group = groupRepository.findById(requestDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        UserEntity author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("Автор не найден"));

        // Проверяем права на публикацию
        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(group.getId(), authorId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (!member.getCanPost() && !member.getIsAdmin() && member.getRole().ordinal() > 2) {
            throw new RuntimeException("Нет прав для публикации постов");
        }

        // Создаем пост
        GroupPostEntity post = GroupPostEntity.builder()
                .group(group)
                .author(author)
                .content(requestDto.getContent())
                .type(requestDto.getType())
                .status(GroupPostStatus.PUBLISHED)
                .publishedAt(Instant.now())
                .build();

        GroupPostEntity savedPost = groupPostRepository.save(post);

        // Обновляем счетчик постов
        updatePostsCount(group.getId());

        log.info("Создан пост ID: {}", savedPost.getId());
        return enrichWithLikedByMe(groupMapper.toPostResponseDto(savedPost), authorId);
    }

    /**
     * Получить пост
     */
    @Transactional(readOnly = true)
    public GroupPostResponseDto getPost(Long postId, Long userId) {
        log.info("Получение поста ID: {} для пользователя {}", postId, userId);

        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Проверяем доступ к группе
        checkGroupAccess(post.getGroup(), userId);

        return enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId);
    }

    /**
     * Получить посты группы
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getGroupPosts(Long groupId, Long userId, Pageable pageable) {
        log.info("Получение постов группы ID: {} для пользователя {}", groupId, userId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем доступ
        checkGroupAccess(group, userId);

        Page<GroupPostEntity> posts = groupPostRepository.findByGroupIdAndStatus(groupId, GroupPostStatus.PUBLISHED, pageable);
        return posts.map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId));
    }

    /**
     * Получить ленту постов пользователя
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getUserFeed(Long userId, Pageable pageable) {
        log.info("Получение ленты пользователя ID: {}", userId);

        Page<GroupPostEntity> posts = groupPostRepository.findFeedForUser(userId, pageable);
        return posts.map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId));
    }

    /**
     * Обновить пост
     */
    @Transactional
    public GroupPostResponseDto updatePost(Long postId, String content, Long userId) {
        log.info("Обновление поста ID: {} пользователем {}", postId, userId);

        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Проверяем права
        if (!post.isAuthor(userId)) {
            GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(post.getGroup().getId(), userId)
                    .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

            if (!member.getCanManagePosts() && !member.getIsAdmin()) {
                throw new RuntimeException("Нет прав для редактирования поста");
            }
        }

        post.setContent(content);
        post.setUpdatedAt(Instant.now());

        GroupPostEntity updatedPost = groupPostRepository.save(post);
        return enrichWithLikedByMe(groupMapper.toPostResponseDto(updatedPost), userId);
    }

    /**
     * Удалить пост
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        log.info("Удаление поста ID: {} пользователем {}", postId, userId);

        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        // Проверяем права
        if (!post.isAuthor(userId)) {
            GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(post.getGroup().getId(), userId)
                    .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

            if (!member.getCanManagePosts() && !member.getIsAdmin()) {
                throw new RuntimeException("Нет прав для удаления поста");
            }
        }

        groupPostRepository.delete(post);

        // Обновляем счетчик постов
        updatePostsCount(post.getGroup().getId());
    }

    /**
     * Поставить/убрать лайк
     */
    @Transactional
    public GroupPostResponseDto toggleLike(Long postId, Long userId) {
        log.info("Переключение лайка для поста {} пользователем {}", postId, userId);

        GroupPostEntity post = groupPostRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Пост не найден"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Проверяем доступ к группе
        checkGroupAccess(post.getGroup(), userId);

        // Ищем существующий лайк
        Optional<GroupPostLikeEntity> existingLike = post.getLikes().stream()
                .filter(like -> like.getUser().getId().equals(userId))
                .findFirst();

        if (existingLike.isPresent()) {
            // Убираем лайк
            post.getLikes().remove(existingLike.get());
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        } else {
            // Ставим лайк
            GroupPostLikeEntity like = GroupPostLikeEntity.builder()
                    .post(post)
                    .user(user)
                    .build();
            post.getLikes().add(like);
            post.setLikesCount(post.getLikesCount() + 1);
        }

        GroupPostEntity updatedPost = groupPostRepository.save(post);
        return enrichWithLikedByMe(groupMapper.toPostResponseDto(updatedPost), userId);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private GroupPostResponseDto enrichWithLikedByMe(GroupPostResponseDto dto, Long userId) {
        if (userId != null) {
            groupPostRepository.findById(dto.getId()).ifPresent(post -> {
                boolean likedByMe = post.getLikes().stream()
                        .anyMatch(like -> like.getUser().getId().equals(userId));
                dto.setLikedByMe(likedByMe);
            });
        }
        return dto;
    }

    private void checkGroupAccess(GroupEntity group, Long userId) {
        if (group.getIsPublic()) {
            return; // Публичные группы доступны всем
        }

        if (!group.isMember(userId)) {
            throw new RuntimeException("Доступ к группе запрещен");
        }
    }

    private void updatePostsCount(Long groupId) {
        long count = groupPostRepository.countByGroupIdAndStatus(groupId, GroupPostStatus.PUBLISHED);
        groupRepository.findById(groupId).ifPresent(group -> {
            group.setPostsCount((int) count);
            groupRepository.save(group);
        });
    }

    /**
     * Получить посты определенного типа в группе
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getPostsByType(Long groupId, GroupPostType type,
                                                     Long userId, Pageable pageable) {
        log.info("Получение постов типа {} в группе {} для пользователя {}", type, groupId, userId);

        // Проверяем доступ к группе
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        checkGroupAccess(group, userId);

        // Получаем посты
        Page<GroupPostEntity> posts = groupPostRepository.findByGroupIdAndType(
                groupId, type, pageable);

        return posts.map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId));
    }

    /**
     * Получить посты пользователя во всех группах
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getPostsByAuthor(Long authorId, Long currentUserId,
                                                       Pageable pageable) {
        log.info("Получение постов автора {} для пользователя {}", authorId, currentUserId);

        // Получаем посты автора
        Page<GroupPostEntity> posts = groupPostRepository.findByAuthorId(authorId, pageable);

        // Фильтруем только те посты, к которым есть доступ
        List<GroupPostEntity> accessiblePosts = posts.stream()
                .filter(post -> {
                    try {
                        checkGroupAccess(post.getGroup(), currentUserId);
                        return true;
                    } catch (RuntimeException e) {
                        return false;
                    }
                })
                .collect(Collectors.toList());

        return new PageImpl<>(accessiblePosts, pageable, accessiblePosts.size())
                .map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), currentUserId));
    }

    /**
     * Получить самые популярные посты в группе (по лайкам)
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getPopularPosts(Long groupId, Long userId, Pageable pageable) {
        log.info("Получение популярных постов в группе {} для пользователя {}", groupId, userId);

        // Проверяем доступ к группе
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        checkGroupAccess(group, userId);

        // Получаем посты, отсортированные по количеству лайков
        Page<GroupPostEntity> posts = groupPostRepository.findPopularPostsByGroupId(
                groupId, pageable);

        return posts.map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId));
    }

    /**
     * Получить посты за определенный период
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getPostsByPeriod(Long groupId, Instant startDate,
                                                       Instant endDate, Long userId, Pageable pageable) {
        log.info("Получение постов в группе {} за период {} - {} для пользователя {}",
                groupId, startDate, endDate, userId);

        // Проверяем доступ к группе
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        checkGroupAccess(group, userId);

        // Получаем посты за период
        Page<GroupPostEntity> posts = groupPostRepository.findByGroupIdAndCreatedAtBetween(
                groupId, startDate, endDate, pageable);

        return posts.map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId));
    }

    /**
     * Получить черновики пользователя в группе
     */
    @Transactional(readOnly = true)
    public Page<GroupPostResponseDto> getDrafts(Long groupId, Long userId, Pageable pageable) {
        log.info("Получение черновиков в группе {} для пользователя {}", groupId, userId);

        // Проверяем, что пользователь является участником группы
        var member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        // Получаем черновики
        Page<GroupPostEntity> drafts = groupPostRepository.findByGroupIdAndAuthorIdAndStatus(
                groupId, userId, GroupPostStatus.DRAFT, pageable);

        return drafts.map(post -> enrichWithLikedByMe(groupMapper.toPostResponseDto(post), userId));
    }
}