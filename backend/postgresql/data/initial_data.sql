TRUNCATE TABLE users CASCADE;

INSERT INTO users (id, login, email, password_hash)
VALUES ('11111111-1111-1111-1111-111111111111', 'designer_anna', 'anna@example.com', 'hashed_pwd_1'),
       ('22222222-2222-2222-2222-222222222222', 'startup_ivan', 'ivan@example.com', 'hashed_pwd_2');

-- Проект 1: Публичный брендбук Анны (в нем есть история чата)
INSERT INTO projects (id, author_id, name, description, chat_history, is_private)
VALUES ('33333333-3333-3333-3333-333333333333',
        '11111111-1111-1111-1111-111111111111',
        'Брендбук кофейни "Зерно"',
        'Логотип в теплых тонах, минимализм',
        '[{"role": "user", "content": "Сделай логотип для кофейни"}]'::jsonb,
        false);

-- Проект 2: Приватный проект Ивана
INSERT INTO projects (id, author_id, name, description, chat_history, is_private)
VALUES ('44444444-4444-4444-4444-444444444444',
        '22222222-2222-2222-2222-222222222222',
        'Секретный крипто-стартап',
        'Футуристичный стиль, неоновые цвета',
        '[]'::jsonb,
        true);


-- У проекта Анны уже есть две генерации (Версия 1 и Версия 2)
INSERT INTO project_versions (project_id, version_num, s3_file_key)
VALUES ('33333333-3333-3333-3333-333333333333', 1, 'mock_s3/coffee_v1_draft.json'),
       ('33333333-3333-3333-3333-333333333333', 2, 'mock_s3/coffee_v2_final.json');

-- У проекта Ивана пока только первая версия
INSERT INTO project_versions (project_id, version_num, s3_file_key)
VALUES ('44444444-4444-4444-4444-444444444444', 1, 'mock_s3/crypto_v1.json');

