package ru.messenger.group_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.group_service.api.dto.request.GroupInviteRequestDto;
import ru.messenger.group_service.api.dto.response.GroupInviteResponseDto;
import ru.messenger.group_service.api.dto.response.GroupMemberResponseDto;
import ru.messenger.group_service.api.mapper.GroupInviteMapper;
import ru.messenger.group_service.domain.entity.GroupEntity;
import ru.messenger.group_service.domain.entity.GroupInviteEntity;
import ru.messenger.group_service.domain.entity.GroupMemberEntity;
import ru.messenger.group_service.domain.entity.enums.GroupInviteStatus;
import ru.messenger.group_service.domain.repository.GroupInviteRepository;
import ru.messenger.group_service.domain.repository.GroupMemberRepository;
import ru.messenger.group_service.domain.repository.GroupRepository;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupInviteService {

    private final GroupInviteRepository groupInviteRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupInviteMapper groupInviteMapper;

    /**
     * Создать приглашение в группу
     */
    @Transactional
    public GroupInviteResponseDto createInvite(GroupInviteRequestDto requestDto, Long inviterId) {
        log.info("Создание приглашения в группу {} пользователем {}",
                requestDto.getGroupId(), inviterId);

        // 1. Находим группу
        GroupEntity group = groupRepository.findById(requestDto.getGroupId())
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // 2. Проверяем права приглашающего
        GroupMemberEntity inviter = groupMemberRepository.findByGroupIdAndUserId(
                        group.getId(), inviterId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (!inviter.getCanInvite() && !inviter.getIsAdmin()) {
            throw new RuntimeException("Нет прав для приглашения участников");
        }

        // 3. Проверяем, приглашаем по userId или по email
        UserEntity invitedUser = null;
        String invitedEmail = null;

        if (requestDto.getUserId() != null) {
            // Приглашение существующего пользователя
            invitedUser = userRepository.findById(requestDto.getUserId())
                    .orElseThrow(() -> new RuntimeException("Приглашаемый пользователь не найден"));

            // Проверяем, не является ли уже участником
            if (groupMemberRepository.existsByGroupIdAndUserId(group.getId(), requestDto.getUserId())) {
                throw new RuntimeException("Пользователь уже является участником группы");
            }
        } else if (requestDto.getEmail() != null && !requestDto.getEmail().trim().isEmpty()) {
            // Приглашение по email
            invitedEmail = requestDto.getEmail().trim();

            // Проверяем формат email
            if (!isValidEmail(invitedEmail)) {
                throw new RuntimeException("Некорректный формат email");
            }
        } else {
            throw new RuntimeException("Необходимо указать userId или email");
        }

        // 4. Проверяем, не было ли уже приглашения
        if (invitedUser != null) {
            Optional<GroupInviteEntity> existingInvite = groupInviteRepository
                    .findByGroupIdAndInvitedIdAndStatus(group.getId(), invitedUser.getId(),
                            GroupInviteStatus.PENDING);
            if (existingInvite.isPresent()) {
                throw new RuntimeException("Приглашение уже отправлено этому пользователю");
            }
        } else {
            Optional<GroupInviteEntity> existingInvite = groupInviteRepository
                    .findByGroupIdAndInvitedEmailAndStatus(group.getId(), invitedEmail,
                            GroupInviteStatus.PENDING);
            if (existingInvite.isPresent()) {
                throw new RuntimeException("Приглашение уже отправлено на этот email");
            }
        }

        // 5. Создаем приглашение
        GroupInviteEntity invite = GroupInviteEntity.builder()
                .group(group)
                .inviter(userRepository.findById(inviterId)
                        .orElseThrow(() -> new RuntimeException("Приглашающий не найден")))
                .invited(invitedUser)
                .invitedEmail(invitedEmail)
                .status(GroupInviteStatus.PENDING)
                .message(requestDto.getMessage())
                .expiresAt(Instant.now().plusSeconds(7 * 24 * 60 * 60)) // 7 дней
                .token(UUID.randomUUID().toString())
                .build();

        GroupInviteEntity savedInvite = groupInviteRepository.save(invite);

        log.info("Создано приглашение ID: {}", savedInvite.getId());
        return groupInviteMapper.toResponseDto(savedInvite);
    }

    /**
     * Принять приглашение по токену
     */
    @Transactional
    public GroupMemberResponseDto acceptInvite(String token, Long userId) {
        log.info("Принятие приглашения по токену {} пользователем {}", token, userId);

        // 1. Находим приглашение
        GroupInviteEntity invite = groupInviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Приглашение не найдено"));

        // 2. Проверяем статус
        if (invite.getStatus() != GroupInviteStatus.PENDING) {
            throw new RuntimeException("Приглашение уже использовано или отменено");
        }

        // 3. Проверяем срок действия
        if (invite.isExpired()) {
            invite.setStatus(GroupInviteStatus.EXPIRED);
            groupInviteRepository.save(invite);
            throw new RuntimeException("Срок действия приглашения истек");
        }

        // 4. Проверяем, предназначено ли приглашение этому пользователю
        if (invite.getInvited() != null && !invite.getInvited().getId().equals(userId)) {
            throw new RuntimeException("Это приглашение предназначено другому пользователю");
        }

        // 5. Находим пользователя
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // 6. Проверяем, не является ли уже участником
        if (groupMemberRepository.existsByGroupIdAndUserId(invite.getGroup().getId(), userId)) {
            throw new RuntimeException("Вы уже являетесь участником этой группы");
        }

        // 7. Добавляем пользователя в группу
        GroupMemberEntity member = addMember(invite.getGroup(), user);

        // 8. Обновляем статус приглашения
        invite.setStatus(GroupInviteStatus.ACCEPTED);
        invite.setInvited(user); // Связываем приглашение с пользователем
        groupInviteRepository.save(invite);

        log.info("Приглашение принято, пользователь {} добавлен в группу {}", userId, invite.getGroup().getId());

        // Нужно вернуть GroupMemberResponseDto, для этого нужен маппер
        return convertToMemberResponseDto(member);
    }

    /**
     * Отклонить приглашение
     */
    @Transactional
    public void declineInvite(String token, Long userId) {
        log.info("Отклонение приглашения по токену {} пользователем {}", token, userId);

        // 1. Находим приглашение
        GroupInviteEntity invite = groupInviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Приглашение не найдено"));

        // 2. Проверяем статус
        if (invite.getStatus() != GroupInviteStatus.PENDING) {
            throw new RuntimeException("Приглашение уже использовано или отменено");
        }

        // 3. Проверяем, предназначено ли приглашение этому пользователю
        if (invite.getInvited() != null && !invite.getInvited().getId().equals(userId)) {
            throw new RuntimeException("Это приглашение предназначено другому пользователю");
        }

        // 4. Обновляем статус
        invite.setStatus(GroupInviteStatus.DECLINED);
        groupInviteRepository.save(invite);

        log.info("Приглашение отклонено");
    }

    /**
     * Отменить приглашение
     */
    @Transactional
    public void cancelInvite(Long inviteId, Long userId) {
        log.info("Отмена приглашения {} пользователем {}", inviteId, userId);

        // 1. Находим приглашение
        GroupInviteEntity invite = groupInviteRepository.findById(inviteId)
                .orElseThrow(() -> new RuntimeException("Приглашение не найдено"));

        // 2. Проверяем права
        if (!invite.getInviter().getId().equals(userId)) {
            // Проверяем, является ли пользователь администратором группы
            var member = groupMemberRepository.findByGroupIdAndUserId(
                    invite.getGroup().getId(), userId);

            if (member.isEmpty() || (!member.get().getIsAdmin() &&
                    member.get().getRole() != ru.messenger.group_service.domain.entity.enums.GroupMemberRole.OWNER)) {
                throw new RuntimeException("Нет прав для отмены приглашения");
            }
        }

        // 3. Обновляем статус
        invite.setStatus(GroupInviteStatus.CANCELLED);
        groupInviteRepository.save(invite);

        log.info("Приглашение отменено");
    }

    /**
     * Получить приглашения группы
     */
    @Transactional(readOnly = true)
    public Page<GroupInviteResponseDto> getGroupInvites(Long groupId, Long userId, Pageable pageable) {
        log.info("Получение приглашений группы {} для пользователя {}", groupId, userId);

        // 1. Проверяем доступ к группе
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // 2. Проверяем права
        var member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (!member.getCanInvite() && !member.getIsAdmin()) {
            throw new RuntimeException("Нет прав для просмотра приглашений");
        }

        // 3. Получаем приглашения
        Page<GroupInviteEntity> invites = groupInviteRepository.findByGroupId(groupId, pageable);

        return invites.map(groupInviteMapper::toResponseDto);
    }

    /**
     * Получить мои приглашения
     */
    @Transactional(readOnly = true)
    public Page<GroupInviteResponseDto> getMyInvites(Long userId, Pageable pageable) {
        log.info("Получение приглашений пользователя {}", userId);

        Page<GroupInviteEntity> invites = groupInviteRepository.findByInvitedId(userId, pageable);

        return invites.map(groupInviteMapper::toResponseDto);
    }

    /**
     * Получить информацию о приглашении по токену
     */
    @Transactional(readOnly = true)
    public GroupInviteResponseDto getInviteByToken(String token) {
        log.info("Получение информации о приглашении по токену {}", token);

        GroupInviteEntity invite = groupInviteRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Приглашение не найдено"));

        return groupInviteMapper.toResponseDto(invite);
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private GroupMemberEntity addMember(GroupEntity group, UserEntity user) {
        GroupMemberEntity member = GroupMemberEntity.builder()
                .group(group)
                .user(user)
                .role(ru.messenger.group_service.domain.entity.enums.GroupMemberRole.MEMBER)
                .status(ru.messenger.group_service.domain.entity.enums.GroupMemberStatus.ACTIVE)
                .isAdmin(false)
                .canPost(true)
                .canInvite(false)
                .canManageUsers(false)
                .canManagePosts(false)
                .joinedAt(Instant.now())
                .build();

        return groupMemberRepository.save(member);
    }

    private GroupMemberResponseDto convertToMemberResponseDto(GroupMemberEntity member) {
        // Создаем простой DTO
        GroupMemberResponseDto dto = new GroupMemberResponseDto();
        dto.setId(member.getId());
        dto.setGroupId(member.getGroup().getId());
        dto.setUserId(member.getUser().getId());
        dto.setUsername(member.getUser().getUsername());
        dto.setAvatarUrl(member.getUser().getAvatarUrl());
        dto.setRole(member.getRole());
        dto.setStatus(member.getStatus());
        dto.setIsAdmin(member.getIsAdmin());
        dto.setCanPost(member.getCanPost());
        dto.setCanInvite(member.getCanInvite());
        dto.setCanManageUsers(member.getCanManageUsers());
        dto.setCanManagePosts(member.getCanManagePosts());
        dto.setJoinedAt(member.getJoinedAt());

        return dto;
    }

    private boolean isValidEmail(String email) {
        // Простая проверка email
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }
}