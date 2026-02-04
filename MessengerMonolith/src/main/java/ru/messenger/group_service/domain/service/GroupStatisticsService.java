package ru.messenger.group_service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.messenger.group_service.domain.entity.enums.GroupMemberStatus;
import ru.messenger.group_service.domain.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroupStatisticsService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final GroupPostRepository groupPostRepository;
    private final GroupCommentRepository groupCommentRepository;

    /**
     * Получить общую статистику платформы
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformStatistics() {
        log.info("Получение статистики платформы");

        Map<String, Object> stats = new HashMap<>();

        // Общая статистика
        stats.put("totalGroups", groupRepository.count());
        stats.put("totalPosts", groupPostRepository.count());
        stats.put("totalComments", groupCommentRepository.count());

        // Статистика по типам групп
        Map<String, Long> groupsByType = new HashMap<>();
        groupsByType.put("GROUP", groupRepository.countByType(
                ru.messenger.group_service.domain.entity.enums.GroupType.GROUP));
        groupsByType.put("CHANNEL", groupRepository.countByType(
                ru.messenger.group_service.domain.entity.enums.GroupType.CHANNEL));
        groupsByType.put("COMMUNITY", groupRepository.countByType(
                ru.messenger.group_service.domain.entity.enums.GroupType.COMMUNITY));

        stats.put("groupsByType", groupsByType);

        // Активность за последние 30 дней
        Instant monthAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);

        long newGroupsLastMonth = groupRepository.countByCreatedAtAfter(monthAgo);
        long newPostsLastMonth = groupPostRepository.countByCreatedAtAfter(monthAgo);
        long newCommentsLastMonth = groupCommentRepository.countByCreatedAtAfter(monthAgo);

        stats.put("newGroupsLastMonth", newGroupsLastMonth);
        stats.put("newPostsLastMonth", newPostsLastMonth);
        stats.put("newCommentsLastMonth", newCommentsLastMonth);

        return stats;
    }

    /**
     * Получить статистику активности группы за период
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getGroupActivityStatistics(Long groupId, Instant startDate,
                                                          Instant endDate, Long userId) {
        log.info("Получение статистики активности группы {} за период {} - {} для пользователя {}",
                groupId, startDate, endDate, userId);

        // Проверяем права (только участники группы)
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new RuntimeException("Доступ запрещен");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("groupId", groupId);
        stats.put("period", Map.of("start", startDate, "end", endDate));

        // Статистика по постам
        long postsCount = groupPostRepository.countByGroupIdAndCreatedAtBetween(
                groupId, startDate, endDate);
        stats.put("postsCount", postsCount);

        // Статистика по комментариям
        long commentsCount = groupCommentRepository.countByGroupIdAndCreatedAtBetween(
                groupId, startDate, endDate);
        stats.put("commentsCount", commentsCount);

        // Активные участники (те, кто оставлял посты или комментарии)
        long activeMembers = groupMemberRepository.countActiveMembersInPeriod(
                groupId, startDate, endDate);
        stats.put("activeMembers", activeMembers);

        // Распределение активности по дням
        Map<LocalDate, Long> activityByDay = getActivityByDay(groupId, startDate, endDate);
        stats.put("activityByDay", activityByDay);

        return stats;
    }

    /**
     * Получить статистику пользователя в группе
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserGroupStatistics(Long groupId, Long targetUserId,
                                                      Long currentUserId) {
        log.info("Получение статистики пользователя {} в группе {} для пользователя {}",
                targetUserId, groupId, currentUserId);

        // Проверяем права (участники группы могут видеть свою статистику,
        // администраторы - любую)
        var currentUserMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentUserId)
                .orElseThrow(() -> new RuntimeException("Вы не являетесь участником группы"));

        var targetUserMember = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new RuntimeException("Целевой пользователь не является участником группы"));

        // Проверяем права доступа
        if (!targetUserId.equals(currentUserId) &&
                !currentUserMember.getIsAdmin() &&
                currentUserMember.getRole() != ru.messenger.group_service.domain.entity.enums.GroupMemberRole.OWNER) {
            throw new RuntimeException("Нет прав для просмотра статистики других пользователей");
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("groupId", groupId);
        stats.put("userId", targetUserId);
        stats.put("username", targetUserMember.getUser().getUsername());

        // Статистика по постам
        long userPostsCount = groupPostRepository.countByGroupIdAndAuthorId(groupId, targetUserId);
        stats.put("postsCount", userPostsCount);

        // Статистика по комментариям
        long userCommentsCount = groupCommentRepository.countByGroupIdAndAuthorId(groupId, targetUserId);
        stats.put("commentsCount", userCommentsCount);

        // Дата вступления
        stats.put("joinedAt", targetUserMember.getJoinedAt());

        // Роль и права
        stats.put("role", targetUserMember.getRole());
        stats.put("isAdmin", targetUserMember.getIsAdmin());
        stats.put("canPost", targetUserMember.getCanPost());
        stats.put("canInvite", targetUserMember.getCanInvite());

        // Активность за последние 30 дней
        Instant monthAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);

        long recentPosts = groupPostRepository.countByGroupIdAndAuthorIdAndCreatedAtAfter(
                groupId, targetUserId, monthAgo);
        long recentComments = groupCommentRepository.countByGroupIdAndAuthorIdAndCreatedAtAfter(
                groupId, targetUserId, monthAgo);

        stats.put("recentPosts", recentPosts);
        stats.put("recentComments", recentComments);

        return stats;
    }

    /**
     * Получить топ активных групп - ИСПРАВЛЕНО
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopActiveGroups(int limit) {
        log.info("Получение топ {} активных групп", limit);

        // Используем метод findTopActiveGroups который теперь существует
        List<ru.messenger.group_service.domain.entity.GroupEntity> groups =
                groupRepository.findTopActiveGroups(limit);

        return groups.stream()
                .map(group -> {
                    Map<String, Object> groupStats = new HashMap<>();
                    groupStats.put("groupId", group.getId());
                    groupStats.put("groupName", group.getName());
                    groupStats.put("groupType", group.getType());
                    groupStats.put("postsCount", group.getPostsCount());
                    groupStats.put("membersCount", group.getMembersCount());
                    groupStats.put("createdAt", group.getCreatedAt());

                    // Можно добавить дополнительную статистику
                    long recentPosts = groupPostRepository.countByGroupIdAndCreatedAtAfter(
                            group.getId(), Instant.now().minusSeconds(7 * 24 * 60 * 60));
                    groupStats.put("recentPostsLast7Days", recentPosts);

                    return groupStats;
                })
                .collect(Collectors.toList());
    }

    /**
     * Получить топ активных пользователей в группе - ИСПРАВЛЕНО
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopActiveUsersInGroup(Long groupId, int limit, Long userId) {
        log.info("Получение топ {} активных пользователей в группе {} для пользователя {}",
                limit, groupId, userId);

        // Проверяем права (только участники группы)
        boolean isMember = groupMemberRepository.existsByGroupIdAndUserId(groupId, userId);
        if (!isMember) {
            throw new RuntimeException("Доступ запрещен");
        }

        // Используем метод findTopActiveUsersInGroup
        List<Object[]> rawResults = groupMemberRepository.findTopActiveUsersInGroup(groupId, limit);

        return rawResults.stream()
                .map(result -> {
                    Map<String, Object> userStats = new HashMap<>();
                    userStats.put("userId", result[0]);
                    userStats.put("username", result[1]);
                    userStats.put("postsCount", result[2]);
                    userStats.put("commentsCount", result[3]);

                    // Вычисляем общую активность
                    Long posts = (Long) result[2];
                    Long comments = (Long) result[3];
                    Long totalActivity = (posts != null ? posts : 0) + (comments != null ? comments : 0);
                    userStats.put("totalActivity", totalActivity);

                    return userStats;
                })
                .collect(Collectors.toList());
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private Map<LocalDate, Long> getActivityByDay(Long groupId, Instant startDate, Instant endDate) {
        // Получаем посты по дням
        List<Object[]> postsByDay = groupPostRepository.countPostsByDay(groupId, startDate, endDate);

        // Получаем комментарии по дням
        List<Object[]> commentsByDay = groupCommentRepository.countCommentsByDay(groupId, startDate, endDate);

        // Объединяем результаты
        Map<LocalDate, Long> activityMap = new HashMap<>();

        // Добавляем посты
        for (Object[] result : postsByDay) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            Long count = ((Number) result[1]).longValue();
            activityMap.put(date, activityMap.getOrDefault(date, 0L) + count);
        }

        // Добавляем комментарии
        for (Object[] result : commentsByDay) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            Long count = ((Number) result[1]).longValue();
            activityMap.put(date, activityMap.getOrDefault(date, 0L) + count);
        }

        return activityMap;
    }
}