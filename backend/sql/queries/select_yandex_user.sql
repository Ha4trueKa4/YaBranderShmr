SELECT id::text, login, email
FROM users
WHERE provider = 'yandex' AND provider_id = $1;
