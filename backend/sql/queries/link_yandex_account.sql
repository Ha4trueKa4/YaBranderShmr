UPDATE users
SET provider = 'yandex', provider_id = $1
WHERE id = $2::uuid AND provider_id IS NULL
RETURNING id;
