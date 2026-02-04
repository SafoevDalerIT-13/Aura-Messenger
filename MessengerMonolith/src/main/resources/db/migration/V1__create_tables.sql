-- ============================================
-- Миграция V1: Создание основных таблиц
-- ============================================

-- Таблица пользователей
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       login VARCHAR(255) NOT NULL UNIQUE,
                       username VARCHAR(255) NOT NULL,
                       email VARCHAR(255) UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       description VARCHAR(500),
                       avatar_url VARCHAR(500),
                       phone_number VARCHAR(20) UNIQUE,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       last_seen_at TIMESTAMP WITH TIME ZONE,
                       is_online BOOLEAN DEFAULT FALSE
);

-- Таблица контактов (КОРРЕКТНАЯ версия под ContactEntity)
CREATE TABLE contacts (
                          id BIGSERIAL PRIMARY KEY,

    -- Внешний ключ на пользователя-владельца
                          user_id BIGINT NOT NULL, -- ← именно user_id, как в @JoinColumn

    -- Поля из ContactEntity
                          type VARCHAR(20) NOT NULL,
                          value VARCHAR(255) NOT NULL,
                          is_primary BOOLEAN DEFAULT FALSE, -- ← именно is_primary

    -- Временные метки (необязательно, но хорошо бы)
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,

    -- Внешний ключ
                          CONSTRAINT fk_contacts_user
                              FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Уникальность: один пользователь не может иметь два одинаковых контакта
                          CONSTRAINT uk_contacts_user_type_value
                              UNIQUE (user_id, type, value)
);

-- Комментарии к таблице contacts
COMMENT ON TABLE contacts IS 'Контактная информация пользователей (телефоны, email и т.д.)';
COMMENT ON COLUMN contacts.user_id IS 'ID пользователя-владельца контакта';
COMMENT ON COLUMN contacts.type IS 'Тип контакта: PHONE, EMAIL, TELEGRAM и т.д.';
COMMENT ON COLUMN contacts.value IS 'Значение контакта (номер телефона, email, username)';
COMMENT ON COLUMN contacts.is_primary IS 'Является ли основным контактом';

-- Индексы для быстрого поиска
CREATE INDEX idx_contacts_user_id ON contacts(user_id);
CREATE INDEX idx_contacts_type ON contacts(type);
CREATE INDEX idx_contacts_is_primary ON contacts(is_primary) WHERE is_primary = true;

-- Таблица для списка друзей/контактов пользователя
CREATE TABLE user_contacts (
                               id BIGSERIAL PRIMARY KEY,

    -- Пользователь, который добавляет в контакты
                               user_id BIGINT NOT NULL,

    -- Пользователь, которого добавляют в контакты
                               friend_id BIGINT NOT NULL,

    -- Псевдоним (кастомное имя для друга)
                               nickname VARCHAR(100),

    -- Статус заявки
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                                   CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED')),

    -- Флаги
                               is_favorite BOOLEAN DEFAULT FALSE,
                               is_blocked BOOLEAN DEFAULT FALSE,

    -- Временные метки
                               added_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               last_interaction_at TIMESTAMP WITH TIME ZONE,

    -- Внешние ключи
                               CONSTRAINT fk_user_contacts_user
                                   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_user_contacts_friend
                                   FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE,

    -- Уникальность: один пользователь может добавить другого только один раз
                               CONSTRAINT uk_user_contacts_user_friend
                                   UNIQUE (user_id, friend_id)
);

-- Комментарии к таблице user_contacts
COMMENT ON TABLE user_contacts IS 'Список контактов/друзей пользователя';
COMMENT ON COLUMN user_contacts.user_id IS 'ID пользователя, который добавляет в контакты';
COMMENT ON COLUMN user_contacts.friend_id IS 'ID пользователя, которого добавляют в контакты';
COMMENT ON COLUMN user_contacts.nickname IS 'Псевдоним для контакта (кастомное имя)';
COMMENT ON COLUMN user_contacts.status IS 'Статус заявки: PENDING, ACCEPTED, REJECTED, BLOCKED';
COMMENT ON COLUMN user_contacts.is_favorite IS 'Является ли контакт избранным';
COMMENT ON COLUMN user_contacts.is_blocked IS 'Заблокирован ли контакт';
COMMENT ON COLUMN user_contacts.added_at IS 'Дата добавления в контакты';
COMMENT ON COLUMN user_contacts.updated_at IS 'Дата последнего обновления';
COMMENT ON COLUMN user_contacts.last_interaction_at IS 'Дата последнего взаимодействия';

-- Индексы для быстрого поиска
CREATE INDEX idx_user_contacts_user_id ON user_contacts(user_id);
CREATE INDEX idx_user_contacts_friend_id ON user_contacts(friend_id);
CREATE INDEX idx_user_contacts_status ON user_contacts(status);
CREATE INDEX idx_user_contacts_is_favorite ON user_contacts(is_favorite) WHERE is_favorite = true;
CREATE INDEX idx_user_contacts_is_blocked ON user_contacts(is_blocked) WHERE is_blocked = true;
CREATE INDEX idx_user_contacts_added_at ON user_contacts(added_at DESC);
CREATE INDEX idx_user_contacts_accepted ON user_contacts(user_id, friend_id) WHERE status = 'ACCEPTED';
CREATE INDEX idx_user_contacts_incoming ON user_contacts(friend_id) WHERE status = 'PENDING';
CREATE INDEX idx_user_contacts_outgoing ON user_contacts(user_id) WHERE status = 'PENDING';

-- Остальные таблицы
CREATE TABLE chats (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100),
                       type VARCHAR(20) NOT NULL CHECK (type IN ('PRIVATE', 'GROUP', 'CHANNEL')),
                       avatar_url VARCHAR(500),
                       last_message_id BIGINT,
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE chat_participants (
                                   chat_id BIGINT NOT NULL,
                                   user_id BIGINT NOT NULL,
                                   joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (chat_id, user_id),
                                   CONSTRAINT fk_chat_participants_chat
                                       FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                                   CONSTRAINT fk_chat_participants_user
                                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE messages (
                          id BIGSERIAL PRIMARY KEY,
                          chat_id BIGINT NOT NULL,
                          sender_id BIGINT NOT NULL,
                          content TEXT,
                          message_type VARCHAR(20) NOT NULL CHECK (message_type IN ('TEXT', 'IMAGE', 'VIDEO', 'FILE', 'AUDIO', 'SYSTEM')),
                          status VARCHAR(20) NOT NULL DEFAULT 'SENT' CHECK (status IN ('SENT', 'DELIVERED', 'READ', 'FAILED')),
                          sent_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT fk_messages_chat
                              FOREIGN KEY (chat_id) REFERENCES chats(id) ON DELETE CASCADE,
                          CONSTRAINT fk_messages_sender
                              FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE message_read_by (
                                 message_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,
                                 read_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                 PRIMARY KEY (message_id, user_id),
                                 CONSTRAINT fk_message_read_by_message
                                     FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE,
                                 CONSTRAINT fk_message_read_by_user
                                     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE attachments (
                             id BIGSERIAL PRIMARY KEY,
                             message_id BIGINT NOT NULL,
                             file_name VARCHAR(255) NOT NULL,
                             file_url VARCHAR(500) NOT NULL,
                             file_type VARCHAR(100),
                             file_size BIGINT,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_attachments_message
                                 FOREIGN KEY (message_id) REFERENCES messages(id) ON DELETE CASCADE
);

-- ============================================
-- Таблицы для групп (добавлены в конец)
-- ============================================

-- Таблица групп
CREATE TABLE groups (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(255) NOT NULL,
                        description VARCHAR(1000),
                        avatar_url VARCHAR(500),
                        cover_url VARCHAR(500),
                        type VARCHAR(20) NOT NULL CHECK (type IN ('GROUP', 'CHANNEL', 'COMMUNITY')),
                        visibility VARCHAR(20) NOT NULL CHECK (visibility IN ('PUBLIC', 'PRIVATE', 'HIDDEN')),
                        owner_id BIGINT NOT NULL,
                        is_public BOOLEAN NOT NULL DEFAULT TRUE,
                        members_count INTEGER NOT NULL DEFAULT 1,
                        posts_count INTEGER NOT NULL DEFAULT 0,
                        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                        CONSTRAINT fk_groups_owner
                            FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица участников группы
CREATE TABLE group_members (
                               id BIGSERIAL PRIMARY KEY,
                               group_id BIGINT NOT NULL,
                               user_id BIGINT NOT NULL,
                               role VARCHAR(20) NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('OWNER', 'ADMIN', 'MODERATOR', 'MEMBER')),
                               status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'BANNED', 'MUTED')),
                               joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               is_admin BOOLEAN DEFAULT FALSE,
                               can_post BOOLEAN DEFAULT TRUE,
                               can_invite BOOLEAN DEFAULT FALSE,
                               can_manage_users BOOLEAN DEFAULT FALSE,
                               can_manage_posts BOOLEAN DEFAULT FALSE,
                               CONSTRAINT fk_group_members_group
                                   FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                               CONSTRAINT fk_group_members_user
                                   FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT uk_group_members_group_user
                                   UNIQUE (group_id, user_id)
);

-- Таблица приглашений в группу
CREATE TABLE group_invites (
                               id BIGSERIAL PRIMARY KEY,
                               group_id BIGINT NOT NULL,
                               inviter_id BIGINT NOT NULL,
                               invited_id BIGINT,
                               invited_email VARCHAR(255),
                               status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED')),
                               message VARCHAR(500),
                               expires_at TIMESTAMP WITH TIME ZONE,
                               token VARCHAR(255) UNIQUE,
                               created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                               CONSTRAINT fk_group_invites_group
                                   FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                               CONSTRAINT fk_group_invites_inviter
                                   FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
                               CONSTRAINT fk_group_invites_invited
                                   FOREIGN KEY (invited_id) REFERENCES users(id) ON DELETE SET NULL
);

-- Таблица постов в группах
CREATE TABLE group_posts (
                             id BIGSERIAL PRIMARY KEY,
                             group_id BIGINT NOT NULL,
                             author_id BIGINT NOT NULL,
                             content VARCHAR(2000),
                             type VARCHAR(20) NOT NULL DEFAULT 'POST' CHECK (type IN ('POST', 'ANNOUNCEMENT', 'NEWS', 'QUESTION')),
                             status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'DELETED')),
                             likes_count INTEGER NOT NULL DEFAULT 0,
                             comments_count INTEGER NOT NULL DEFAULT 0,
                             shares_count INTEGER NOT NULL DEFAULT 0,
                             created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                             published_at TIMESTAMP WITH TIME ZONE,
                             CONSTRAINT fk_group_posts_group
                                 FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE,
                             CONSTRAINT fk_group_posts_author
                                 FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица лайков постов
CREATE TABLE group_post_likes (
                                  id BIGSERIAL PRIMARY KEY,
                                  post_id BIGINT NOT NULL,
                                  user_id BIGINT NOT NULL,
                                  created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_group_post_likes_post
                                      FOREIGN KEY (post_id) REFERENCES group_posts(id) ON DELETE CASCADE,
                                  CONSTRAINT fk_group_post_likes_user
                                      FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                  CONSTRAINT uk_group_post_likes_post_user
                                      UNIQUE (post_id, user_id)
);

-- Таблица комментариев к постам
CREATE TABLE group_post_comments (
                                     id BIGSERIAL PRIMARY KEY,
                                     post_id BIGINT NOT NULL,
                                     author_id BIGINT NOT NULL,
                                     content VARCHAR(1000) NOT NULL,
                                     parent_comment_id BIGINT,
                                     likes_count INTEGER NOT NULL DEFAULT 0,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_group_post_comments_post
                                         FOREIGN KEY (post_id) REFERENCES group_posts(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_group_post_comments_author
                                         FOREIGN KEY (author_id) REFERENCES users(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_group_post_comments_parent
                                         FOREIGN KEY (parent_comment_id) REFERENCES group_post_comments(id) ON DELETE CASCADE
);

-- Таблица лайков комментариев
CREATE TABLE group_comment_likes (
                                     id BIGSERIAL PRIMARY KEY,
                                     comment_id BIGINT NOT NULL,
                                     user_id BIGINT NOT NULL,
                                     created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT fk_group_comment_likes_comment
                                         FOREIGN KEY (comment_id) REFERENCES group_post_comments(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_group_comment_likes_user
                                         FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                                     CONSTRAINT uk_group_comment_likes_comment_user
                                         UNIQUE (comment_id, user_id)
);