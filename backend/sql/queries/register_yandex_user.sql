INSERT INTO users (login, email, password_hash, provider, provider_id)
VALUES ($1, $2, NULL, 'yandex', $3)
RETURNING id::text, login, email;
