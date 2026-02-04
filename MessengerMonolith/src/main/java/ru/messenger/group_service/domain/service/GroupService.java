package ru.messenger.group_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.group_service.api.dto.request.GroupCreateRequestDto;
import ru.messenger.group_service.api.dto.request.GroupUpdateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupMemberResponseDto;
import ru.messenger.group_service.api.dto.response.GroupResponseDto;
import ru.messenger.group_service.api.mapper.GroupMapper;
import ru.messenger.group_service.domain.entity.GroupEntity;
import ru.messenger.group_service.domain.entity.GroupMemberEntity;
import ru.messenger.group_service.domain.entity.enums.GroupMemberRole;
import ru.messenger.group_service.domain.entity.enums.GroupMemberStatus;
import ru.messenger.group_service.domain.entity.enums.GroupType;
import ru.messenger.group_service.domain.repository.GroupMemberRepository;
import ru.messenger.group_service.domain.repository.GroupRepository;
import ru.messenger.user_service.domain.entity.UserEntity;
import ru.messenger.user_service.domain.repository.UserRepository;
import ru.messenger.user_service.domain.service.exception.UserNotFoundException;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;
    private final GroupMapper groupMapper;

    /**
     * Создать новую группу
     */
    @Transactional
    public GroupResponseDto createGroup(GroupCreateRequestDto requestDto, Long creatorId) {
        log.info("Создание группы пользователем ID: {}", creatorId);

        UserEntity creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new UserNotFoundException("Создатель не найден"));

        // Создаем группу
        GroupEntity group = groupMapper.toEntity(requestDto);
        group.setOwner(creator);
        group.setIsPublic(requestDto.getVisibility() == ru.messenger.group_service.domain.entity.enums.GroupVisibility.PUBLIC);

        GroupEntity savedGroup = groupRepository.save(group);

        // Добавляем создателя как владельца
        addMember(savedGroup, creator, GroupMemberRole.OWNER, true, true, true, true);

        // Добавляем начальных участников если есть
        if (requestDto.getInitialMembers() != null && !requestDto.getInitialMembers().isEmpty()) {
            addInitialMembers(savedGroup, creator, requestDto.getInitialMembers());
        }

        log.info("Создана группа ID: {}", savedGroup.getId());
        return groupMapper.toResponseDto(savedGroup);
    }

    /**
     * Получить информацию о группе
     */
    @Transactional(readOnly = true)
    public GroupResponseDto getGroup(Long groupId, Long userId) {
        log.info("Получение информации о группе ID: {} для пользователя {}", groupId, userId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем доступ
        checkGroupAccess(group, userId);

        return groupMapper.toResponseDto(group);
    }

    /**
     * Обновить информацию о группе
     */
    @Transactional
    public GroupResponseDto updateGroup(Long groupId, GroupUpdateRequestDto requestDto, Long userId) {
        log.info("Обновление группы ID: {} пользователем {}", groupId, userId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем права доступа
        if (!group.isOwner(userId) && !group.isAdmin(userId)) {
            throw new RuntimeException("Нет прав для редактирования группы");
        }

        groupMapper.updateEntity(requestDto, group);
        GroupEntity updatedGroup = groupRepository.save(group);

        return groupMapper.toResponseDto(updatedGroup);
    }

    /**
     * Получить список групп пользователя
     */
    @Transactional(readOnly = true)
    public Page<GroupResponseDto> getUserGroups(Long userId, Pageable pageable) {
        log.info("Получение групп пользователя ID: {}", userId);

        Page<GroupEntity> groups = groupRepository.findByUserId(userId, pageable);
        return groups.map(groupMapper::toResponseDto);
    }

    /**
     * Получить список публичных групп
     */
    @Transactional(readOnly = true)
    public Page<GroupResponseDto> getPublicGroups(ru.messenger.group_service.domain.entity.enums.GroupType type, Pageable pageable) {
        log.info("Получение публичных групп типа: {}", type);

        Page<GroupEntity> groups = groupRepository.findByTypeAndIsPublicTrue(type, pageable);
        return groups.map(groupMapper::toResponseDto);
    }

    /**
     * Присоединиться к группе
     */
    @Transactional
    public GroupMemberResponseDto joinGroup(Long groupId, Long userId) {
        log.info("Пользователь ID: {} присоединяется к группе ID: {}", userId, groupId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        // Проверяем возможность присоединения
        if (group.getVisibility() == ru.messenger.group_service.domain.entity.enums.GroupVisibility.PRIVATE) {
            throw new RuntimeException("К этой группе можно присоединиться только по приглашению");
        }

        // Проверяем, не является ли уже участником
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new RuntimeException("Вы уже являетесь участником этой группы");
        }

        // Добавляем как обычного участника
        GroupMemberEntity member = addMember(group, user, GroupMemberRole.MEMBER, true, false, false, false);

        return groupMapper.toMemberResponseDto(member);
    }

    /**
     * Покинуть группу
     */
    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        log.info("Пользователь ID: {} покидает группу ID: {}", userId, groupId);

        GroupMemberEntity member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником этой группы"));

        // Владелец не может покинуть группу, он должен передать права или удалить группу
        if (member.getRole() == GroupMemberRole.OWNER) {
            throw new RuntimeException("Владелец не может покинуть группу. Передайте права или удалите группу");
        }

        member.setStatus(GroupMemberStatus.LEFT);
        groupMemberRepository.save(member);

        // Обновляем счетчик участников
        updateMembersCount(groupId);
    }

    /**
     * Добавить участника в группу
     */
    @Transactional
    public GroupMemberResponseDto addMemberToGroup(Long groupId, Long targetUserId, Long inviterId) {
        log.info("Добавление участника {} в группу {} пригласителем {}", targetUserId, groupId, inviterId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем права пригласителя
        GroupMemberEntity inviter = groupMemberRepository.findByGroupIdAndUserId(groupId, inviterId)
                .orElseThrow(() -> new RuntimeException("Пригласитель не является участником группы"));

        if (!inviter.getCanInvite() && inviter.getRole() != GroupMemberRole.OWNER && !inviter.getIsAdmin()) {
            throw new RuntimeException("Нет прав для приглашения участников");
        }

        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException("Пользователь не найден"));

        // Проверяем, не является ли уже участником
        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, targetUserId)) {
            throw new RuntimeException("Пользователь уже является участником группы");
        }

        // Добавляем как обычного участника
        GroupMemberEntity member = addMember(group, targetUser, GroupMemberRole.MEMBER, true, false, false, false);

        return groupMapper.toMemberResponseDto(member);
    }

    /**
     * Удалить участника из группы
     */
    @Transactional
    public void removeMemberFromGroup(Long groupId, Long targetUserId, Long removerId) {
        log.info("Удаление участника {} из группы {} пользователем {}", targetUserId, groupId, removerId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем права удаляющего
        GroupMemberEntity remover = groupMemberRepository.findByGroupIdAndUserId(groupId, removerId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        GroupMemberEntity targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Участник не найден"));

        // Проверяем права
        if (targetMember.getRole() == GroupMemberRole.OWNER) {
            throw new RuntimeException("Нельзя удалить владельца группы");
        }

        if (!remover.getCanManageUsers() && remover.getRole() != GroupMemberRole.OWNER && !remover.getIsAdmin()) {
            throw new RuntimeException("Нет прав для удаления участников");
        }

        // Нельзя удалить пользователя с равными или большими правами
        if (targetMember.getRole().ordinal() <= remover.getRole().ordinal() &&
                remover.getRole() != GroupMemberRole.OWNER) {
            throw new RuntimeException("Нельзя удалить пользователя с равными или большими правами");
        }

        targetMember.setStatus(GroupMemberStatus.BANNED);
        groupMemberRepository.save(targetMember);

        // Обновляем счетчик участников
        updateMembersCount(groupId);
    }

    /**
     * Получить список участников группы
     */
    @Transactional(readOnly = true)
    public Page<GroupMemberResponseDto> getGroupMembers(Long groupId, Long userId, Pageable pageable) {
        log.info("Получение участников группы ID: {} для пользователя {}", groupId, userId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем доступ
        checkGroupAccess(group, userId);

        Page<GroupMemberEntity> members = groupMemberRepository.findByGroupId(groupId, pageable);
        return members.map(groupMapper::toMemberResponseDto);
    }

    /**
     * Изменить роль участника
     */
    @Transactional
    public GroupMemberResponseDto changeMemberRole(Long groupId, Long targetUserId,
                                                   GroupMemberRole newRole, Long changerId) {
        log.info("Изменение роли участника {} в группе {} на {} пользователем {}",
                targetUserId, groupId, newRole, changerId);

        GroupMemberEntity changer = groupMemberRepository.findByGroupIdAndUserId(groupId, changerId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        GroupMemberEntity targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Участник не найден"));

        // Проверяем права
        if (!changer.getCanManageUsers() && changer.getRole() != GroupMemberRole.OWNER && !changer.getIsAdmin()) {
            throw new RuntimeException("Нет прав для изменения ролей");
        }

        // Нельзя изменить роль владельца
        if (targetMember.getRole() == GroupMemberRole.OWNER) {
            throw new RuntimeException("Нельзя изменить роль владельца");
        }

        // Нельзя изменять роль пользователю с равными или большими правами
        if (targetMember.getRole().ordinal() <= changer.getRole().ordinal() &&
                changer.getRole() != GroupMemberRole.OWNER) {
            throw new RuntimeException("Нельзя изменить роль пользователя с равными или большими правами");
        }

        targetMember.setRole(newRole);
        targetMember.setIsAdmin(newRole == GroupMemberRole.ADMIN || newRole == GroupMemberRole.OWNER);
        targetMember.setCanInvite(newRole == GroupMemberRole.ADMIN || newRole == GroupMemberRole.OWNER);
        targetMember.setCanManageUsers(newRole == GroupMemberRole.ADMIN || newRole == GroupMemberRole.OWNER);
        targetMember.setCanManagePosts(newRole == GroupMemberRole.ADMIN || newRole == GroupMemberRole.MODERATOR || newRole == GroupMemberRole.OWNER);

        GroupMemberEntity updated = groupMemberRepository.save(targetMember);
        return groupMapper.toMemberResponseDto(updated);
    }

    /**
     * Передать права владельца
     */
    @Transactional
    public void transferOwnership(Long groupId, Long newOwnerId, Long currentOwnerId) {
        log.info("Передача прав владельца группы {} от {} к {}", groupId, currentOwnerId, newOwnerId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем, что текущий пользователь является владельцем
        if (!group.isOwner(currentOwnerId)) {
            throw new RuntimeException("Только владелец может передать права");
        }

        // Проверяем, что новый владелец является участником
        GroupMemberEntity newOwnerMember = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId)
                .orElseThrow(() -> new RuntimeException("Новый владелец не является участником группы"));

        // Находим текущего владельца
        GroupMemberEntity currentOwnerMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentOwnerId)
                .orElseThrow(() -> new RuntimeException("Текущий владелец не найден"));

        // Меняем роли
        currentOwnerMember.setRole(GroupMemberRole.ADMIN);
        currentOwnerMember.setIsAdmin(true);
        currentOwnerMember.setCanInvite(true);
        currentOwnerMember.setCanManageUsers(true);
        currentOwnerMember.setCanManagePosts(true);

        newOwnerMember.setRole(GroupMemberRole.OWNER);
        newOwnerMember.setIsAdmin(true);
        newOwnerMember.setCanInvite(true);
        newOwnerMember.setCanManageUsers(true);
        newOwnerMember.setCanManagePosts(true);

        groupMemberRepository.saveAll(List.of(currentOwnerMember, newOwnerMember));

        // Обновляем владельца в группе
        UserEntity newOwner = userRepository.findById(newOwnerId)
                .orElseThrow(() -> new UserNotFoundException("Новый владелец не найден"));
        group.setOwner(newOwner);
        groupRepository.save(group);

        log.info("Права владельца успешно переданы");
    }

    /**
     * Удалить группу
     */
    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        log.info("Удаление группы ID: {} пользователем {}", groupId, userId);

        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        // Проверяем права
        if (!group.isOwner(userId)) {
            throw new RuntimeException("Только владелец может удалить группу");
        }

        groupRepository.delete(group);
        log.info("Группа удалена");
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private GroupMemberEntity addMember(GroupEntity group, UserEntity user,
                                        GroupMemberRole role,
                                        Boolean canPost, Boolean canInvite,
                                        Boolean canManageUsers, Boolean canManagePosts) {
        GroupMemberEntity member = GroupMemberEntity.builder()
                .group(group)
                .user(user)
                .role(role)
                .status(GroupMemberStatus.ACTIVE)
                .isAdmin(role == GroupMemberRole.ADMIN || role == GroupMemberRole.OWNER)
                .canPost(canPost)
                .canInvite(canInvite)
                .canManageUsers(canManageUsers)
                .canManagePosts(canManagePosts)
                .joinedAt(Instant.now())
                .build();

        GroupMemberEntity savedMember = groupMemberRepository.save(member);

        // Обновляем счетчик участников
        updateMembersCount(group.getId());

        return savedMember;
    }

    private void addInitialMembers(GroupEntity group, UserEntity inviter, Set<Long> userIds) {
        for (Long userId : userIds) {
            if (userId.equals(inviter.getId())) {
                continue; // Пропускаем создателя
            }

            userRepository.findById(userId).ifPresent(user -> {
                if (!groupMemberRepository.existsByGroupIdAndUserId(group.getId(), userId)) {
                    addMember(group, user, GroupMemberRole.MEMBER, true, false, false, false);
                }
            });
        }
    }

    private void updateMembersCount(Long groupId) {
        long count = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        groupRepository.findById(groupId).ifPresent(group -> {
            group.setMembersCount((int) count);
            groupRepository.save(group);
        });
    }

    private void checkGroupAccess(GroupEntity group, Long userId) {
        if (group.getIsPublic()) {
            return; // Публичные группы доступны всем
        }

        if (!group.isMember(userId)) {
            throw new RuntimeException("Доступ к группе запрещен");
        }
    }

    /**
     * Поиск групп по названию или описанию
     */
    @Transactional(readOnly = true)
    public Page<GroupResponseDto> searchGroups(String query, GroupType type, Long userId, Pageable pageable) {
        log.info("Поиск групп по запросу '{}', тип: {}, пользователь: {}", query, type, userId);

        String searchQuery = query.toLowerCase();

        Page<GroupEntity> groups;
        if (type != null) {
            // Поиск по типу и публичности
            groups = groupRepository.searchPublicGroups(type, searchQuery, pageable);
        } else {
            // Поиск по всем типам
            groups = groupRepository.searchAllPublicGroups(searchQuery, pageable);
        }

        return groups.map(groupMapper::toResponseDto);
    }

    /**
     * Получить количество групп, созданных пользователем
     */
    @Transactional(readOnly = true)
    public long getUserGroupsCount(Long userId) {
        return groupRepository.countByOwnerId(userId);
    }

    /**
     * Получить группы, где пользователь является администратором
     */
    @Transactional(readOnly = true)
    public Page<GroupResponseDto> getAdminGroups(Long userId, Pageable pageable) {
        log.info("Получение групп, где пользователь {} является администратором", userId);

        // Получаем все группы пользователя
        Page<GroupEntity> userGroups = groupRepository.findByUserId(userId, pageable);

        // Фильтруем только те, где пользователь админ
        List<GroupEntity> adminGroups = userGroups.stream()
                .filter(group -> {
                    var member = groupMemberRepository.findByGroupIdAndUserId(group.getId(), userId);
                    return member.isPresent() &&
                            (member.get().getIsAdmin() || member.get().getRole() == GroupMemberRole.OWNER);
                })
                .collect(Collectors.toList());

        return new PageImpl<>(adminGroups, pageable, adminGroups.size())
                .map(groupMapper::toResponseDto);
    }

    /**
     * Получить статистику по группе
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupStatistics(Long groupId, Long userId) {
        log.info("Получение статистики группы {} для пользователя {}", groupId, userId);

        // Проверяем доступ
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Группа не найдена"));

        if (!group.isMember(userId)) {
            throw new RuntimeException("Доступ запрещен");
        }

        // Проверяем права (только владелец и админы могут видеть статистику)
        var member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        if (!member.getIsAdmin() && member.getRole() != GroupMemberRole.OWNER) {
            throw new RuntimeException("Нет прав для просмотра статистики");
        }

        Map<String, Object> statistics = new HashMap<>();

        // Основная статистика
        statistics.put("groupId", groupId);
        statistics.put("groupName", group.getName());
        statistics.put("membersCount", group.getMembersCount());
        statistics.put("postsCount", group.getPostsCount());
        statistics.put("createdAt", group.getCreatedAt());

        // Статистика по участникам
        long activeMembers = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.ACTIVE);
        long bannedMembers = groupMemberRepository.countByGroupIdAndStatus(groupId, GroupMemberStatus.BANNED);

        statistics.put("activeMembers", activeMembers);
        statistics.put("bannedMembers", bannedMembers);

        // Статистика по ролям
        List<GroupMemberEntity> allMembers = groupMemberRepository.findByGroupId(groupId);
        Map<GroupMemberRole, Long> rolesCount = allMembers.stream()
                .collect(Collectors.groupingBy(GroupMemberEntity::getRole, Collectors.counting()));

        statistics.put("rolesDistribution", rolesCount);

        // Статистика по активности (последние 7 дней)
        Instant weekAgo = Instant.now().minusSeconds(7 * 24 * 60 * 60);

        // Здесь можно добавить логику для подсчета активности

        return statistics;
    }
}