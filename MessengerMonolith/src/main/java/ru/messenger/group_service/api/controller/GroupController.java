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
import ru.messenger.group_service.api.dto.request.GroupCreateRequestDto;
import ru.messenger.group_service.api.dto.request.GroupUpdateRequestDto;
import ru.messenger.group_service.api.dto.response.GroupMemberResponseDto;
import ru.messenger.group_service.api.dto.response.GroupResponseDto;
import ru.messenger.group_service.domain.entity.enums.GroupType;
import ru.messenger.group_service.domain.service.GroupService;
import ru.messenger.user_service.domain.service.UserService;

@RestController
@RequestMapping("/api/v1/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;
    private final UserService userService;

    /**
     * Создать новую группу
     *
     * @param requestDto данные для создания группы
     * @param userDetails информация о текущем пользователе
     * @return созданная группа
     */
    @PostMapping
    public ResponseEntity<GroupResponseDto> createGroup(
            @Valid @RequestBody GroupCreateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupResponseDto response = groupService.createGroup(requestDto, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить информацию о группе
     *
     * @param groupId ID группы
     * @param userDetails информация о текущем пользователе
     * @return информация о группе
     */
    @GetMapping("/{groupId}")
    public ResponseEntity<GroupResponseDto> getGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupResponseDto response = groupService.getGroup(groupId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Обновить информацию о группе
     *
     * @param groupId ID группы
     * @param requestDto новые данные группы
     * @param userDetails информация о текущем пользователе
     * @return обновленная группа
     */
    @PutMapping("/{groupId}")
    public ResponseEntity<GroupResponseDto> updateGroup(
            @PathVariable Long groupId,
            @Valid @RequestBody GroupUpdateRequestDto requestDto,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupResponseDto response = groupService.updateGroup(groupId, requestDto, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить список моих групп
     *
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с моими группами
     */
    @GetMapping("/my")
    public ResponseEntity<Page<GroupResponseDto>> getMyGroups(
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupResponseDto> response = groupService.getUserGroups(userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Получить список публичных групп по типу
     *
     * @param type тип группы (GROUP, CHANNEL, COMMUNITY)
     * @param pageable параметры пагинации
     * @return страница с публичными группами
     */
    @GetMapping("/public")
    public ResponseEntity<Page<GroupResponseDto>> getPublicGroups(
            @RequestParam GroupType type,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<GroupResponseDto> response = groupService.getPublicGroups(type, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Присоединиться к группе
     *
     * @param groupId ID группы
     * @param userDetails информация о текущем пользователе
     * @return информация о новом участнике
     */
    @PostMapping("/{groupId}/join")
    public ResponseEntity<GroupMemberResponseDto> joinGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        GroupMemberResponseDto response = groupService.joinGroup(groupId, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Покинуть группу
     *
     * @param groupId ID группы
     * @param userDetails информация о текущем пользователе
     * @return пустой ответ
     */
    @PostMapping("/{groupId}/leave")
    public ResponseEntity<Void> leaveGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        groupService.leaveGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Добавить участника в группу
     *
     * @param groupId ID группы
     * @param userId ID добавляемого пользователя
     * @param userDetails информация о текущем пользователе (пригласителе)
     * @return информация о новом участнике
     */
    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<GroupMemberResponseDto> addMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long inviterId = userService.getCurrentUserId(userDetails);
        GroupMemberResponseDto response = groupService.addMemberToGroup(groupId, userId, inviterId);
        return ResponseEntity.ok(response);
    }

    /**
     * Удалить участника из группы
     *
     * @param groupId ID группы
     * @param userId ID удаляемого пользователя
     * @param userDetails информация о текущем пользователе
     * @return пустой ответ
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long groupId,
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long removerId = userService.getCurrentUserId(userDetails);
        groupService.removeMemberFromGroup(groupId, userId, removerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Получить список участников группы
     *
     * @param groupId ID группы
     * @param pageable параметры пагинации
     * @param userDetails информация о текущем пользователе
     * @return страница с участниками группы
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<Page<GroupMemberResponseDto>> getMembers(
            @PathVariable Long groupId,
            @PageableDefault(size = 50) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        Page<GroupMemberResponseDto> response = groupService.getGroupMembers(groupId, userId, pageable);
        return ResponseEntity.ok(response);
    }

    /**
     * Изменить роль участника группы
     *
     * @param groupId ID группы
     * @param targetUserId ID пользователя, чью роль меняем
     * @param newRole новая роль
     * @param userDetails информация о текущем пользователе
     * @return информация об обновленном участнике
     */
    @PutMapping("/{groupId}/members/{targetUserId}/role")
    public ResponseEntity<GroupMemberResponseDto> changeMemberRole(
            @PathVariable Long groupId,
            @PathVariable Long targetUserId,
            @RequestParam ru.messenger.group_service.domain.entity.enums.GroupMemberRole newRole,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long changerId = userService.getCurrentUserId(userDetails);
        GroupMemberResponseDto response = groupService.changeMemberRole(groupId, targetUserId, newRole, changerId);
        return ResponseEntity.ok(response);
    }

    /**
     * Передать права владельца группы
     *
     * @param groupId ID группы
     * @param newOwnerId ID нового владельца
     * @param userDetails информация о текущем пользователе (текущем владельце)
     * @return пустой ответ
     */
    @PostMapping("/{groupId}/transfer-ownership/{newOwnerId}")
    public ResponseEntity<Void> transferOwnership(
            @PathVariable Long groupId,
            @PathVariable Long newOwnerId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long currentOwnerId = userService.getCurrentUserId(userDetails);
        groupService.transferOwnership(groupId, newOwnerId, currentOwnerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Удалить группу
     *
     * @param groupId ID группы
     * @param userDetails информация о текущем пользователе
     * @return пустой ответ
     */
    @DeleteMapping("/{groupId}")
    public ResponseEntity<Void> deleteGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        groupService.deleteGroup(groupId, userId);
        return ResponseEntity.ok().build();
    }

    /**
     * Поиск групп по названию или описанию
     *
     * @param query поисковый запрос
     * @param type тип группы (опционально)
     * @param pageable параметры пагинации
     * @return страница с найденными группами
     */
    @GetMapping("/search")
    public ResponseEntity<Page<GroupResponseDto>> searchGroups(
            @RequestParam String query,
            @RequestParam(required = false) GroupType type,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = userService.getCurrentUserId(userDetails);
        // Можно добавить отдельный метод в сервисе для поиска
        Page<GroupResponseDto> response = groupService.searchGroups(query, type, userId, pageable);
        return ResponseEntity.ok(response);
    }
}